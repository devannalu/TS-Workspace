package com.devannalu.tsworkspace.reunioes;

import com.devannalu.tsworkspace.reunioes.ReuniaoRepository.*;
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
public class ReuniaoService {
    public static final String CONFLITO="Esta reunião foi atualizada. Atualize os dados e tente novamente.";
    private final ReuniaoRepository reunioes;
    private final TarefaRepository equipes;
    private final PermissaoService permissoes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    public ReuniaoService(ReuniaoRepository reunioes,TarefaRepository equipes,PermissaoService permissoes,BloqueioOrganizacao bloqueio,AuditoriaRepository auditoria) {
        this.reunioes=reunioes;this.equipes=equipes;this.permissoes=permissoes;this.bloqueio=bloqueio;this.auditoria=auditoria;
    }
    public record Dados(String titulo,String pauta,Tipo tipo,Status status,String equipeId,LocalDateTime inicioLocal,
        LocalDateTime fimLocal,String zona,String local,String link,String resultados,List<String> participanteIds,List<String> responsavelIds) { }
    public record Capacidades(boolean editar,boolean arquivar) { }
    public record Resposta(String id,String titulo,String pauta,Tipo tipo,Status status,Referencia equipe,
        Instant inicio,Instant fim,String zona,LocalDateTime inicioLocal,LocalDateTime fimLocal,String local,String link,String resultados,
        List<Participante> participantes,Referencia criadaPor,long versao,Instant criadaEm,Instant atualizadaEm,boolean arquivada,Capacidades capacidades) { }
    public record Pagina(List<Resposta> items,long total,int page,int size) { }
    public record Opcoes(List<TarefaRepository.Referencia> equipes,List<TarefaRepository.Referencia> participantes) { }
    private PoliticaTarefa.Acesso acesso(String pessoa,String chave) {
        var atuais=permissoes.buscarPermissoesUsuario(pessoa);var chaves=Set.copyOf(atuais.chavesEfetivas());
        if(atuais.role()==null||!chaves.contains(chave))throw new AccessDeniedException("Acesso negado.");
        return new PoliticaTarefa.Acesso(pessoa,atuais.role().key(),chaves);
    }
    private Registro buscar(PoliticaTarefa.Acesso acesso,String id) {
        var registro=reunioes.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Reunião não encontrada."));
        PoliticaTarefa.exigirEquipe(acesso,equipes.integrante(registro.equipe().id(),acesso.usuarioId()));return registro;
    }
    private void equipe(PoliticaTarefa.Acesso acesso,String id,boolean ativa) {
        if(id==null)throw new IllegalArgumentException();
        boolean arquivada=equipes.equipeArquivada(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Equipe não encontrada."));
        PoliticaTarefa.exigirEquipe(acesso,equipes.integrante(id,acesso.usuarioId()));
        if(ativa&&arquivada)throw ProblemaDominio.conflito("Equipe arquivada: reuniões históricas são somente leitura.");
    }
    private Resposta resposta(PoliticaTarefa.Acesso acesso,Registro r,List<Participante> participantes) {
        boolean alteravel=r.arquivadaEm()==null&&!r.equipeArquivada()&&!acesso.perfil().equals("SUPPORT");
        var zona=ZoneId.of(r.zona());
        return new Resposta(r.id(),r.titulo(),r.pauta(),r.tipo(),r.status(),r.equipe(),r.inicio(),r.fim(),r.zona(),
            LocalDateTime.ofInstant(r.inicio(),zona),LocalDateTime.ofInstant(r.fim(),zona),r.local(),r.link(),r.resultados(),participantes,r.criadaPor(),r.versao(),r.criadaEm(),r.atualizadaEm(),r.arquivadaEm()!=null,
            new Capacidades(alteravel&&acesso.permissoes().contains("meetings.edit"),alteravel&&acesso.permissoes().contains("meetings.archive")));
    }
    @Transactional(readOnly=true)
    public Resposta detalhe(String pessoa,String id) {
        var acesso=acesso(pessoa,"meetings.view");var r=buscar(acesso,id);return resposta(acesso,r,reunioes.participantes(List.of(id)).getOrDefault(id,List.of()));
    }
    @Transactional(readOnly=true)
    public Pagina listar(String pessoa,String equipe,Status status,String busca,boolean arquivadas,int pagina,int tamanho) {
        if(pagina<0||pagina>100000||tamanho<1||tamanho>100||(busca!=null&&busca.length()>160))throw new IllegalArgumentException();
        var acesso=acesso(pessoa,"meetings.view");var consulta=reunioes.consulta(pessoa,acesso.global(),equipe,status,busca,arquivadas,null);
        var registros=reunioes.listar(consulta,pagina,tamanho);var participantes=reunioes.participantes(registros.stream().map(Registro::id).toList());
        return new Pagina(registros.stream().map(r->resposta(acesso,r,participantes.getOrDefault(r.id(),List.of()))).toList(),reunioes.contar(consulta),pagina,tamanho);
    }
    @Transactional(readOnly=true)
    public Opcoes opcoes(String pessoa,String equipe) {
        var acesso=acesso(pessoa,"meetings.view");if(equipe!=null)equipe(acesso,equipe,true);
        return new Opcoes(equipes.listarEquipesDisponiveis(acesso),equipe==null?List.of():equipes.listarResponsaveisElegiveis(equipe));
    }
    public static Instant resolverHorario(LocalDateTime data,String zona) {return HorarioEncontro.resolver(data,zona);}
    private void validar(Dados d) {
        if(d==null||d.titulo()==null||d.titulo().isBlank()||d.titulo().trim().length()>200||d.tipo()==null||d.status()==null)throw new IllegalArgumentException();
        if((d.pauta()!=null&&d.pauta().length()>5000)||(d.resultados()!=null&&d.resultados().length()>5000)||(d.local()!=null&&d.local().length()>500))throw new IllegalArgumentException();
        if(d.link()!=null&&!d.link().isBlank()) {
            if(d.link().length()>2048)throw new IllegalArgumentException();
            try {var uri=new java.net.URI(d.link());if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();}
            catch(java.net.URISyntaxException e){throw new IllegalArgumentException();}
        }
        Instant inicio=resolverHorario(d.inicioLocal(),d.zona()),fim=resolverHorario(d.fimLocal(),d.zona());
        if(!fim.isAfter(inicio)||Duration.between(inicio,fim).toDays()>7)throw new IllegalArgumentException();
    }
    private Set<String> ids(List<String> ids) {
        if(ids==null)return new LinkedHashSet<>();
        if(ids.size()>50||ids.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException();return new LinkedHashSet<>(ids);
    }
    private void salvarParticipantes(String id,Dados d) {
        var responsaveis=ids(d.responsavelIds());var pessoas=ids(d.participanteIds());pessoas.addAll(responsaveis);
        if(pessoas.size()>50||equipes.contarResponsaveisElegiveis(d.equipeId(),pessoas)!=pessoas.size())throw ProblemaDominio.conflito("Selecione integrantes ativas da equipe da reunião.");
        reunioes.substituirParticipantes(id,pessoas,responsaveis);
    }
    @Transactional
    public Resposta criar(String pessoa,Dados d) {
        bloqueio.adquirir();var acesso=acesso(pessoa,"meetings.create");if(acesso.perfil().equals("SUPPORT"))throw new AccessDeniedException("Acesso negado.");
        validar(d);equipe(acesso,d.equipeId(),true);String id=UUID.randomUUID().toString();
        reunioes.inserir(id,d,pessoa,resolverHorario(d.inicioLocal(),d.zona()),resolverHorario(d.fimLocal(),d.zona()));salvarParticipantes(id,d);
        auditoria.registrar(pessoa,"meeting.created","Meeting",id);return detalhe(pessoa,id);
    }
    private Registro alteravel(String pessoa,String id,long versao,boolean arquivo) {
        var acesso=acesso(pessoa,arquivo?"meetings.archive":"meetings.edit");var atual=buscar(acesso,id);
        if(acesso.perfil().equals("SUPPORT"))throw new AccessDeniedException("Acesso negado.");
        if(atual.arquivadaEm()!=null||atual.equipeArquivada())throw ProblemaDominio.conflito("Reunião arquivada: somente leitura.");
        if(versao<0)throw new IllegalArgumentException();if(atual.versao()!=versao)throw ProblemaDominio.conflito(CONFLITO);return atual;
    }
    @Transactional
    public Resposta editar(String pessoa,String id,Dados d,long versao) {
        bloqueio.adquirir();var atual=alteravel(pessoa,id,versao,false);validar(d);equipe(acesso(pessoa,"meetings.edit"),d.equipeId(),true);
        if(reunioes.atualizar(atual,d,resolverHorario(d.inicioLocal(),d.zona()),resolverHorario(d.fimLocal(),d.zona()))!=1)throw ProblemaDominio.conflito(CONFLITO);
        salvarParticipantes(id,d);auditoria.registrar(pessoa,atual.status()!=d.status()?"meeting.status_changed":"meeting.updated","Meeting",id);return detalhe(pessoa,id);
    }
    @Transactional
    public Resposta arquivar(String pessoa,String id,long versao) {
        bloqueio.adquirir();var atual=alteravel(pessoa,id,versao,true);
        if(reunioes.arquivar(atual)!=1)throw ProblemaDominio.conflito(CONFLITO);
        auditoria.registrar(pessoa,"meeting.archived","Meeting",id);return detalhe(pessoa,id);
    }
}
