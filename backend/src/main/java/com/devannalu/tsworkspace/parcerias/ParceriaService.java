package com.devannalu.tsworkspace.parcerias;

import com.devannalu.tsworkspace.parcerias.ParceriaRepository.*;
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
public class ParceriaService {
    public static final String CONFLITO="Esta parceria foi atualizada. Atualize os dados e tente novamente.";
    private final ParceriaRepository parcerias;
    private final TarefaRepository equipes;
    private final PermissaoService permissoes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    public ParceriaService(ParceriaRepository parcerias,TarefaRepository equipes,PermissaoService permissoes,BloqueioOrganizacao bloqueio,AuditoriaRepository auditoria) {
        this.parcerias=parcerias;this.equipes=equipes;this.permissoes=permissoes;this.bloqueio=bloqueio;this.auditoria=auditoria;
    }
    public record Dados(String organizacao,String descricao,Status status,String equipeId,
        String responsavelId,LocalDate dataProximaAcao,String contatoPrincipal,String email,String telefone,String proximaAcao,String observacoes,String eventoId,String projetoId) { }
    public record Capacidades(boolean editar,boolean arquivar,boolean verEvento,boolean verProjeto) { }
    public record Resposta(String id,String organizacao,String descricao,Status status,Referencia equipe,
        Referencia responsavel,LocalDate dataProximaAcao,String contatoPrincipal,String email,String telefone,String proximaAcao,String observacoes,String eventoId,String projetoId,Referencia criadaPor,long versao,
        Instant criadaEm,Instant atualizadaEm,boolean arquivada,Capacidades capacidades) { }
    public record Pagina(List<Resposta> items,long total,int page,int size) { }
    public record Opcoes(List<TarefaRepository.Referencia> equipes,List<TarefaRepository.Referencia> responsaveis,
        List<Referencia> eventos,List<Referencia> projetos) { }
    private PoliticaTarefa.Acesso acesso(String pessoa,String chave) {
        var atuais=permissoes.buscarPermissoesUsuario(pessoa);var chaves=Set.copyOf(atuais.chavesEfetivas());
        if(atuais.role()==null||!chaves.contains(chave))throw new AccessDeniedException("Acesso negado.");
        return new PoliticaTarefa.Acesso(pessoa,atuais.role().key(),chaves);
    }
    private Registro buscar(PoliticaTarefa.Acesso acesso,String id) {
        var r=parcerias.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Parceria não encontrada."));
        PoliticaTarefa.exigirEquipe(acesso,equipes.integrante(r.equipe().id(),acesso.usuarioId()));return r;
    }
    private void equipe(PoliticaTarefa.Acesso acesso,String id) {
        var arquivada=equipes.equipeArquivada(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Equipe não encontrada."));
        PoliticaTarefa.exigirEquipe(acesso,equipes.integrante(id,acesso.usuarioId()));
        if(arquivada)throw ProblemaDominio.conflito("Equipe arquivada: parceria histórica é somente leitura.");
    }
    private Resposta resposta(PoliticaTarefa.Acesso acesso,Registro r) {
        boolean alteravel=r.arquivadaEm()==null&&!r.equipeArquivada()&&!acesso.perfil().equals("SUPPORT");
        return new Resposta(r.id(),r.organizacao(),r.descricao(),r.status(),r.equipe(),r.responsavel(),r.dataProximaAcao(),r.contatoPrincipal(),r.email(),r.telefone(),r.proximaAcao(),r.observacoes(),
            r.eventoId(),r.projetoId(),r.criadaPor(),r.versao(),r.criadaEm(),r.atualizadaEm(),r.arquivadaEm()!=null,
            new Capacidades(alteravel&&acesso.permissoes().contains("partnerships.edit"),alteravel&&acesso.permissoes().contains("partnerships.archive"),
                acesso.permissoes().contains("events.view"),acesso.permissoes().contains("projects.view")));
    }
    @Transactional(readOnly=true)
    public Resposta detalhe(String pessoa,String id) {var a=acesso(pessoa,"partnerships.view");return resposta(a,buscar(a,id));}
    @Transactional(readOnly=true)
    public Pagina listar(String pessoa,String equipe,Status status,String busca,boolean arquivadas,int pagina,int tamanho) {
        if(pagina<0||pagina>100000||tamanho<1||tamanho>100||(busca!=null&&busca.length()>160))throw new IllegalArgumentException();
        var a=acesso(pessoa,"partnerships.view");var q=parcerias.consulta(pessoa,a.global(),equipe,status,busca,arquivadas,null);
        return new Pagina(parcerias.listar(q,pagina,tamanho).stream().map(r->resposta(a,r)).toList(),parcerias.contar(q),pagina,tamanho);
    }
    @Transactional(readOnly=true)
    public Opcoes opcoes(String pessoa,String equipe) {
        var a=acesso(pessoa,"partnerships.view");if(equipe!=null)equipe(a,equipe);
        return new Opcoes(equipes.listarEquipesDisponiveis(a),equipe==null?List.of():equipes.listarResponsaveisElegiveis(equipe),
            equipe!=null&&a.permissoes().contains("events.view")?parcerias.eventos(equipe):List.of(),
            equipe!=null&&a.permissoes().contains("projects.view")?parcerias.projetos(equipe):List.of());
    }
    private void validar(PoliticaTarefa.Acesso acesso,Dados d,Registro anterior) {
        if(d==null||d.status()==null||d.equipeId()==null)throw new IllegalArgumentException();
        PoliticaTarefa.validarTitulo(d.organizacao());PoliticaTarefa.validarDescricao(d.descricao());equipe(acesso,d.equipeId());
        if((d.contatoPrincipal()!=null&&d.contatoPrincipal().length()>200)||(d.email()!=null&&d.email().length()>254)||(d.telefone()!=null&&d.telefone().length()>50)||(d.proximaAcao()!=null&&d.proximaAcao().length()>500)||(d.observacoes()!=null&&d.observacoes().length()>5000))throw new IllegalArgumentException();
        if(d.email()!=null&&!d.email().isBlank()&&!d.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new IllegalArgumentException();
        if(d.dataProximaAcao()!=null&&(d.dataProximaAcao().getYear()<1000||d.dataProximaAcao().getYear()>9999))throw new IllegalArgumentException();
        if(d.responsavelId()!=null&&equipes.contarResponsaveisElegiveis(d.equipeId(),Set.of(d.responsavelId()))!=1)
            throw ProblemaDominio.conflito("Selecione uma responsável ativa da equipe da parceria.");
        // Vínculos históricos já existentes são preservados; novos vínculos exigem leitura da fonte e mesma equipe.
        boolean mesmoEvento=anterior!=null&&Objects.equals(anterior.eventoId(),d.eventoId())&&anterior.equipe().id().equals(d.equipeId());
        boolean mesmoProjeto=anterior!=null&&Objects.equals(anterior.projetoId(),d.projetoId())&&anterior.equipe().id().equals(d.equipeId());
        if(d.eventoId()!=null&&!mesmoEvento) {
            if(!acesso.permissoes().contains("events.view"))throw new AccessDeniedException("Acesso negado.");
            if(!parcerias.eventoAtivo(d.eventoId(),d.equipeId()))throw ProblemaDominio.conflito("Vincule um evento ativo da mesma equipe.");
        }
        if(d.projetoId()!=null&&!mesmoProjeto) {
            if(!acesso.permissoes().contains("projects.view"))throw new AccessDeniedException("Acesso negado.");
            if(!parcerias.projetoAtivo(d.projetoId(),d.equipeId()))throw ProblemaDominio.conflito("Vincule um projeto ativo da mesma equipe.");
        }
    }
    @Transactional
    public Resposta criar(String pessoa,Dados d) {
        bloqueio.adquirir();var a=acesso(pessoa,"partnerships.create");if(a.perfil().equals("SUPPORT"))throw new AccessDeniedException("Acesso negado.");
        validar(a,d,null);var id=UUID.randomUUID().toString();parcerias.inserir(id,d,pessoa);
        auditoria.registrar(pessoa,"partnerships.created","Partnership",id);return detalhe(pessoa,id);
    }
    private Registro alteravel(String pessoa,String id,long versao,boolean arquivo) {
        var a=acesso(pessoa,arquivo?"partnerships.archive":"partnerships.edit");var r=buscar(a,id);
        if(a.perfil().equals("SUPPORT"))throw new AccessDeniedException("Acesso negado.");
        if(r.arquivadaEm()!=null||r.equipeArquivada())throw ProblemaDominio.conflito("Parceria arquivada: somente leitura.");
        if(versao<0)throw new IllegalArgumentException();if(r.versao()!=versao)throw ProblemaDominio.conflito(CONFLITO);return r;
    }
    @Transactional
    public Resposta editar(String pessoa,String id,Dados d,long versao) {
        bloqueio.adquirir();var r=alteravel(pessoa,id,versao,false);validar(acesso(pessoa,"partnerships.edit"),d,r);
        if(parcerias.atualizar(r,d)!=1)throw ProblemaDominio.conflito(CONFLITO);
        auditoria.registrar(pessoa,r.status()!=d.status()?"partnerships.status_changed":"partnerships.updated","Partnership",id);return detalhe(pessoa,id);
    }
    @Transactional
    public Resposta arquivar(String pessoa,String id,long versao) {
        bloqueio.adquirir();var r=alteravel(pessoa,id,versao,true);if(parcerias.arquivar(r)!=1)throw ProblemaDominio.conflito(CONFLITO);
        auditoria.registrar(pessoa,"partnerships.archived","Partnership",id);return detalhe(pessoa,id);
    }
}
