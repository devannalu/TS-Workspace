package com.devannalu.tsworkspace.eventos;

import com.devannalu.tsworkspace.eventos.EventoRepository.*;
import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.tarefas.TarefaRepository;
import com.devannalu.tsworkspace.tarefas.PoliticaTarefa;
import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import com.devannalu.tsworkspace.compartilhado.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService {
    public static final String CONFLITO="Este evento foi atualizado. Atualize os dados e tente novamente.";
    private final EventoRepository eventos;
    private final TarefaRepository equipes;
    private final PermissaoService permissoes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    public EventoService(EventoRepository eventos,TarefaRepository equipes,PermissaoService permissoes,BloqueioOrganizacao bloqueio,AuditoriaRepository auditoria) {
        this.eventos=eventos;this.equipes=equipes;this.permissoes=permissoes;this.bloqueio=bloqueio;this.auditoria=auditoria;
    }
    public record Dados(String nome,String descricao,Formato formato,Status status,String equipeId,LocalDateTime inicioLocal,
        LocalDateTime fimLocal,String zona,String local,String link,String notas,List<String> responsavelIds) { }
    public record Capacidades(boolean editar,boolean arquivar) { }
    public record Resposta(String id,String nome,String descricao,Formato formato,Status status,Referencia equipe,
        Instant inicio,Instant fim,String zona,LocalDateTime inicioLocal,LocalDateTime fimLocal,String local,String link,String notas,
        List<Responsavel> responsaveis,Referencia criadaPor,long versao,Instant criadaEm,Instant atualizadaEm,boolean arquivada,Capacidades capacidades) { }
    public record Pagina(List<Resposta> items,long total,int page,int size) { }
    public record Opcoes(List<TarefaRepository.Referencia> equipes,List<TarefaRepository.Referencia> responsaveis) { }
    private PoliticaTarefa.Acesso acesso(String pessoa,String chave) {
        var atuais=permissoes.buscarPermissoesUsuario(pessoa);var chaves=Set.copyOf(atuais.chavesEfetivas());
        if(atuais.role()==null||!chaves.contains(chave))throw new AccessDeniedException("Acesso negado.");
        return new PoliticaTarefa.Acesso(pessoa,atuais.role().key(),chaves);
    }
    private Registro buscar(PoliticaTarefa.Acesso acesso,String id) {
        var registro=eventos.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Evento não encontrado."));
        PoliticaTarefa.exigirEquipe(acesso,equipes.integrante(registro.equipe().id(),acesso.usuarioId()));return registro;
    }
    private void equipe(PoliticaTarefa.Acesso acesso,String id,boolean ativa) {
        if(id==null)throw new IllegalArgumentException();
        boolean arquivada=equipes.equipeArquivada(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Equipe não encontrada."));
        PoliticaTarefa.exigirEquipe(acesso,equipes.integrante(id,acesso.usuarioId()));
        if(ativa&&arquivada)throw ProblemaDominio.conflito("Equipe arquivada: eventos históricas são somente leitura.");
    }
    private Resposta resposta(PoliticaTarefa.Acesso acesso,Registro r,List<Responsavel> responsaveis) {
        boolean alteravel=r.arquivadaEm()==null&&!r.equipeArquivada()&&!acesso.perfil().equals("SUPPORT");
        var zona=ZoneId.of(r.zona());
        return new Resposta(r.id(),r.nome(),r.descricao(),r.formato(),r.status(),r.equipe(),r.inicio(),r.fim(),r.zona(),
            LocalDateTime.ofInstant(r.inicio(),zona),LocalDateTime.ofInstant(r.fim(),zona),r.local(),r.link(),r.notas(),responsaveis,r.criadaPor(),r.versao(),r.criadaEm(),r.atualizadaEm(),r.arquivadaEm()!=null,
            new Capacidades(alteravel&&acesso.permissoes().contains("events.edit"),alteravel&&acesso.permissoes().contains("events.archive")));
    }
    @Transactional(readOnly=true)
    public Resposta detalhe(String pessoa,String id) {
        var acesso=acesso(pessoa,"events.view");var r=buscar(acesso,id);return resposta(acesso,r,eventos.responsaveis(List.of(id)).getOrDefault(id,List.of()));
    }
    @Transactional(readOnly=true)
    public Pagina listar(String pessoa,String equipe,Status status,String busca,boolean arquivadas,int pagina,int tamanho) {
        if(pagina<0||pagina>100000||tamanho<1||tamanho>100||(busca!=null&&busca.length()>160))throw new IllegalArgumentException();
        var acesso=acesso(pessoa,"events.view");var consulta=eventos.consulta(pessoa,acesso.global(),equipe,status,busca,arquivadas,null);
        var registros=eventos.listar(consulta,pagina,tamanho);var responsaveis=eventos.responsaveis(registros.stream().map(Registro::id).toList());
        return new Pagina(registros.stream().map(r->resposta(acesso,r,responsaveis.getOrDefault(r.id(),List.of()))).toList(),eventos.contar(consulta),pagina,tamanho);
    }
    @Transactional(readOnly=true)
    public Opcoes opcoes(String pessoa,String equipe) {
        var acesso=acesso(pessoa,"events.view");if(equipe!=null)equipe(acesso,equipe,true);
        return new Opcoes(equipes.listarEquipesDisponiveis(acesso),equipe==null?List.of():equipes.listarResponsaveisElegiveis(equipe));
    }
    private static Instant resolverHorario(LocalDateTime data,String zona) {return HorarioEncontro.resolver(data,zona);}
    private void validar(Dados d) {
        if(d==null||d.nome()==null||d.nome().isBlank()||d.nome().trim().length()>200||d.formato()==null||d.status()==null)throw new IllegalArgumentException();
        if((d.descricao()!=null&&d.descricao().length()>5000)||(d.notas()!=null&&d.notas().length()>5000)||(d.local()!=null&&d.local().length()>500))throw new IllegalArgumentException();
        if(d.link()!=null&&!d.link().isBlank()) {
            if(d.link().length()>2048)throw new IllegalArgumentException();
            try {var uri=new java.net.URI(d.link());if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();}
            catch(java.net.URISyntaxException e){throw new IllegalArgumentException();}
        }
        Instant inicio=resolverHorario(d.inicioLocal(),d.zona()),fim=resolverHorario(d.fimLocal(),d.zona());
        if(!fim.isAfter(inicio)||Duration.between(inicio,fim).compareTo(Duration.ofDays(30))>0)throw new IllegalArgumentException();
    }
    private Set<String> ids(List<String> ids) {
        if(ids==null)return new LinkedHashSet<>();
        if(ids.size()>50||ids.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException();return new LinkedHashSet<>(ids);
    }
    private void salvarResponsaveis(String id,Dados d) {
        var responsaveis=ids(d.responsavelIds());
        if(equipes.contarResponsaveisElegiveis(d.equipeId(),responsaveis)!=responsaveis.size())throw ProblemaDominio.conflito("Selecione integrantes ativas da equipe do evento.");
        eventos.substituirResponsaveis(id,responsaveis);
    }
    @Transactional
    public Resposta criar(String pessoa,Dados d) {
        bloqueio.adquirir();var acesso=acesso(pessoa,"events.create");if(acesso.perfil().equals("SUPPORT"))throw new AccessDeniedException("Acesso negado.");
        validar(d);equipe(acesso,d.equipeId(),true);String id=UUID.randomUUID().toString();
        eventos.inserir(id,d,pessoa,resolverHorario(d.inicioLocal(),d.zona()),resolverHorario(d.fimLocal(),d.zona()));salvarResponsaveis(id,d);
        auditoria.registrar(pessoa,"event.created","Event",id);return detalhe(pessoa,id);
    }
    private Registro alteravel(String pessoa,String id,long versao,boolean arquivo) {
        var acesso=acesso(pessoa,arquivo?"events.archive":"events.edit");var atual=buscar(acesso,id);
        if(acesso.perfil().equals("SUPPORT"))throw new AccessDeniedException("Acesso negado.");
        if(atual.arquivadaEm()!=null||atual.equipeArquivada())throw ProblemaDominio.conflito("Evento arquivado: somente leitura.");
        if(versao<0)throw new IllegalArgumentException();if(atual.versao()!=versao)throw ProblemaDominio.conflito(CONFLITO);return atual;
    }
    @Transactional
    public Resposta editar(String pessoa,String id,Dados d,long versao) {
        bloqueio.adquirir();var atual=alteravel(pessoa,id,versao,false);validar(d);equipe(acesso(pessoa,"events.edit"),d.equipeId(),true);
        if(eventos.atualizar(atual,d,resolverHorario(d.inicioLocal(),d.zona()),resolverHorario(d.fimLocal(),d.zona()))!=1)throw ProblemaDominio.conflito(CONFLITO);
        salvarResponsaveis(id,d);auditoria.registrar(pessoa,atual.status()!=d.status()?"event.status_changed":"event.updated","Event",id);return detalhe(pessoa,id);
    }
    @Transactional
    public Resposta arquivar(String pessoa,String id,long versao) {
        bloqueio.adquirir();var atual=alteravel(pessoa,id,versao,true);
        if(eventos.arquivar(atual)!=1)throw ProblemaDominio.conflito(CONFLITO);
        auditoria.registrar(pessoa,"event.archived","Event",id);return detalhe(pessoa,id);
    }
}
