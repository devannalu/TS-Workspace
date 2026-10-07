package com.devannalu.tsworkspace.calendario;

import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.tarefas.*;
import com.devannalu.tsworkspace.projetos.*;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarioService {
    public enum Tipo { TAREFA, PROJETO, REUNIAO, EVENTO }
    public record Referencia(String id, String nome) { }
    public record Item(String id, Tipo tipo, String recursoId, String titulo, LocalDate dataInicio,
        LocalDate dataFim, Referencia equipe, String status, String prioridade,
        List<Referencia> responsaveis, boolean concluido, boolean atrasado,Instant inicioEm,Instant fimEm,String zona) {
        public Item(String id,Tipo tipo,String recursoId,String titulo,LocalDate dataInicio,LocalDate dataFim,
            Referencia equipe,String status,String prioridade,List<Referencia> responsaveis,boolean concluido,boolean atrasado) {
            this(id,tipo,recursoId,titulo,dataInicio,dataFim,equipe,status,prioridade,responsaveis,concluido,atrasado,null,null,null);
        }
    }
    private final CalendarioRepository calendario;
    private final TarefaRepository tarefas;
    private final ProjetoRepository projetos;
    private final PermissaoService permissoes;
    private final com.devannalu.tsworkspace.reunioes.ReuniaoRepository reunioes;
    private final com.devannalu.tsworkspace.eventos.EventoRepository eventos;
    public CalendarioService(CalendarioRepository calendario, TarefaRepository tarefas,
        ProjetoRepository projetos, PermissaoService permissoes,com.devannalu.tsworkspace.reunioes.ReuniaoRepository reunioes,com.devannalu.tsworkspace.eventos.EventoRepository eventos) {
        this.calendario = calendario; this.tarefas = tarefas; this.projetos = projetos; this.permissoes = permissoes;this.reunioes=reunioes;this.eventos=eventos;
    }

    @Transactional(readOnly = true)
    public List<Item> listar(String usuariaId, LocalDate de, LocalDate ate, String equipeId,
        Set<Tipo> tipos, String responsavelId) {
        if (de == null || ate == null || de.isAfter(ate) || ChronoUnit.DAYS.between(de, ate) >= 366)
            throw ProblemaDominio.requisicaoInvalida("Informe um intervalo válido de até 366 dias.");
        var acesso = permissoes.buscarPermissoesUsuario(usuariaId);
        Set<String> chaves = Set.copyOf(acesso.chavesEfetivas());
        if (acesso.role() == null || (!chaves.contains("tasks.view") && !chaves.contains("projects.view") && !chaves.contains("meetings.view") && !chaves.contains("events.view")))
            throw new AccessDeniedException("Acesso negado.");
        var selecionados = tipos == null || tipos.isEmpty() ? EnumSet.allOf(Tipo.class) : tipos;
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Bahia"));
        List<Item> itens = new ArrayList<>();
        if (chaves.contains("tasks.view") && selecionados.contains(Tipo.TAREFA)) {
            var escopo = new PoliticaTarefa.Acesso(usuariaId, acesso.role().key(), chaves);
            var filtros = new TarefaService.FiltrosTarefas(equipeId, null, null, responsavelId, null, de, ate, false, 0, 25);
            var registros = calendario.tarefas(tarefas.montarConsulta(escopo, filtros));
            var responsaveis = tarefas.buscarResponsaveis(registros.stream().map(CalendarioRepository.Registro::id).toList());
            for (var registro : registros) itens.add(item(registro, Tipo.TAREFA,
                responsaveis.getOrDefault(registro.id(), List.of()).stream().map(p -> new Referencia(p.id(), p.nome())).toList(), hoje));
        }
        if (chaves.contains("projects.view") && selecionados.contains(Tipo.PROJETO)) {
            var escopo = new PoliticaProjeto.Acesso(usuariaId, acesso.role().key(), chaves);
            var filtros = new ProjetoService.FiltrosProjetos(equipeId, null, responsavelId, null, null, null, null, null, false, 0, 25);
            var registros = calendario.projetos(projetos.montarConsulta(escopo, filtros), de, ate);
            var responsaveis = projetos.buscarResponsaveis(registros.stream().map(CalendarioRepository.Registro::id).toList());
            for (var registro : registros) itens.add(item(registro, Tipo.PROJETO,
                responsaveis.getOrDefault(registro.id(), List.of()).stream().map(p -> new Referencia(p.id(), p.nome())).toList(), hoje));
        }
        if(chaves.contains("meetings.view")&&selecionados.contains(Tipo.REUNIAO)) {
            var escopo=new PoliticaTarefa.Acesso(usuariaId,acesso.role().key(),chaves);
            var consulta=reunioes.consulta(usuariaId,escopo.global(),equipeId,null,null,false,responsavelId);
            // A margem inclui todas as zonas; a interseção exata usa a data local do encontro.
            var registros=reunioes.calendario(consulta,de.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),ate.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant());
            var participantes=reunioes.participantes(registros.stream().map(com.devannalu.tsworkspace.reunioes.ReuniaoRepository.Registro::id).toList());
            for(var r:registros) {
                var zona=ZoneId.of(r.zona());var inicio=r.inicio().atZone(zona).toLocalDate();var fim=r.fim().minusNanos(1).atZone(zona).toLocalDate();
                if(inicio.isAfter(ate)||fim.isBefore(de)||r.status()==com.devannalu.tsworkspace.reunioes.ReuniaoRepository.Status.CANCELADA)continue;
                itens.add(new Item("REUNIAO:"+r.id(),Tipo.REUNIAO,r.id(),r.titulo(),inicio,fim,new Referencia(r.equipe().id(),r.equipe().nome()),r.status().name(),null,
                    participantes.getOrDefault(r.id(),List.of()).stream().filter(com.devannalu.tsworkspace.reunioes.ReuniaoRepository.Participante::responsavel).map(p->new Referencia(p.id(),p.nome())).toList(),
                    r.status()==com.devannalu.tsworkspace.reunioes.ReuniaoRepository.Status.REALIZADA,false,r.inicio(),r.fim(),r.zona()));
            }
        }
        if(chaves.contains("events.view")&&selecionados.contains(Tipo.EVENTO)) {
            var escopo=new PoliticaTarefa.Acesso(usuariaId,acesso.role().key(),chaves);
            var consulta=eventos.consulta(usuariaId,escopo.global(),equipeId,null,null,false,responsavelId);
            // A margem inclui todas as zonas; a interseção exata usa a data local do encontro.
            var registros=eventos.calendario(consulta,de.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),ate.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant());
            var responsaveis=eventos.responsaveis(registros.stream().map(com.devannalu.tsworkspace.eventos.EventoRepository.Registro::id).toList());
            for(var r:registros) {
                var zona=ZoneId.of(r.zona());var inicio=r.inicio().atZone(zona).toLocalDate();var fim=r.fim().minusNanos(1).atZone(zona).toLocalDate();
                if(inicio.isAfter(ate)||fim.isBefore(de)||r.status()==com.devannalu.tsworkspace.eventos.EventoRepository.Status.CANCELADO)continue;
                itens.add(new Item("EVENTO:"+r.id(),Tipo.EVENTO,r.id(),r.nome(),inicio,fim,new Referencia(r.equipe().id(),r.equipe().nome()),r.status().name(),null,
                    responsaveis.getOrDefault(r.id(),List.of()).stream().map(p->new Referencia(p.id(),p.nome())).toList(),
                    r.status()==com.devannalu.tsworkspace.eventos.EventoRepository.Status.CONCLUIDO,false,r.inicio(),r.fim(),r.zona()));
            }
        }
        return itens.stream().sorted(Comparator.comparing(Item::dataInicio).thenComparing(Item::id)).toList();
    }

    private Item item(CalendarioRepository.Registro registro, Tipo tipo, List<Referencia> responsaveis, LocalDate hoje) {
        boolean concluido = tipo == Tipo.TAREFA ? registro.status().equals("CONCLUIDA") : registro.status().equals("CONCLUIDO");
        boolean atrasado = tipo == Tipo.TAREFA
            ? PoliticaTarefa.atrasada(registro.fim(), Tarefa.Status.valueOf(registro.status()), hoje)
            : registro.possuiFim() && registro.fim().isBefore(hoje) && !concluido;
        // Projeto com apenas início não possui prazo final para ser considerado atrasado.
        return new Item(tipo.name() + ":" + registro.id(), tipo, registro.id(), registro.titulo(), registro.inicio(), registro.fim(),
            new Referencia(registro.equipeId(), registro.equipeNome()), registro.status(), registro.prioridade(), responsaveis, concluido, atrasado);
    }
}
