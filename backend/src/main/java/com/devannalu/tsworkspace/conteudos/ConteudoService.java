package com.devannalu.tsworkspace.conteudos;

import com.devannalu.tsworkspace.conteudos.ConteudoRepository.*;
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
public class ConteudoService {
    public static final String CONFLITO="Este conteúdo foi atualizado. Atualize os dados e tente novamente.";
    private final ConteudoRepository conteudos;
    private final TarefaRepository equipes;
    private final PermissaoService permissoes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    public ConteudoService(ConteudoRepository conteudos,TarefaRepository equipes,PermissaoService permissoes,BloqueioOrganizacao bloqueio,AuditoriaRepository auditoria) {
        this.conteudos=conteudos;this.equipes=equipes;this.permissoes=permissoes;this.bloqueio=bloqueio;this.auditoria=auditoria;
    }
    public record Dados(String titulo,String briefing,Canal canal,Formato formato,Status status,String equipeId,
        String responsavelId,LocalDate publicacaoPlanejada,String eventoId,String projetoId) { }
    public record Capacidades(boolean editar,boolean arquivar,boolean verEvento,boolean verProjeto) { }
    public record Resposta(String id,String titulo,String briefing,Canal canal,Formato formato,Status status,Referencia equipe,
        Referencia responsavel,LocalDate publicacaoPlanejada,String eventoId,String projetoId,Referencia criadaPor,long versao,
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
        var r=conteudos.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Conteúdo não encontrado."));
        PoliticaTarefa.exigirEquipe(acesso,equipes.integrante(r.equipe().id(),acesso.usuarioId()));return r;
    }
    private void equipe(PoliticaTarefa.Acesso acesso,String id) {
        var arquivada=equipes.equipeArquivada(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Equipe não encontrada."));
        PoliticaTarefa.exigirEquipe(acesso,equipes.integrante(id,acesso.usuarioId()));
        if(arquivada)throw ProblemaDominio.conflito("Equipe arquivada: conteúdo histórico é somente leitura.");
    }
    private Resposta resposta(PoliticaTarefa.Acesso acesso,Registro r) {
        boolean alteravel=r.arquivadaEm()==null&&!r.equipeArquivada()&&!acesso.perfil().equals("SUPPORT");
        return new Resposta(r.id(),r.titulo(),r.briefing(),r.canal(),r.formato(),r.status(),r.equipe(),r.responsavel(),r.publicacaoPlanejada(),
            r.eventoId(),r.projetoId(),r.criadaPor(),r.versao(),r.criadaEm(),r.atualizadaEm(),r.arquivadaEm()!=null,
            new Capacidades(alteravel&&acesso.permissoes().contains("content.edit"),alteravel&&acesso.permissoes().contains("content.archive"),
                acesso.permissoes().contains("events.view"),acesso.permissoes().contains("projects.view")));
    }
    @Transactional(readOnly=true)
    public Resposta detalhe(String pessoa,String id) {var a=acesso(pessoa,"content.view");return resposta(a,buscar(a,id));}
    @Transactional(readOnly=true)
    public Pagina listar(String pessoa,String equipe,Status status,String busca,boolean arquivadas,int pagina,int tamanho) {
        if(pagina<0||pagina>100000||tamanho<1||tamanho>100||(busca!=null&&busca.length()>160))throw new IllegalArgumentException();
        var a=acesso(pessoa,"content.view");var q=conteudos.consulta(pessoa,a.global(),equipe,status,busca,arquivadas,null);
        return new Pagina(conteudos.listar(q,pagina,tamanho).stream().map(r->resposta(a,r)).toList(),conteudos.contar(q),pagina,tamanho);
    }
    @Transactional(readOnly=true)
    public Opcoes opcoes(String pessoa,String equipe) {
        var a=acesso(pessoa,"content.view");if(equipe!=null)equipe(a,equipe);
        return new Opcoes(equipes.listarEquipesDisponiveis(a),equipe==null?List.of():equipes.listarResponsaveisElegiveis(equipe),
            equipe!=null&&a.permissoes().contains("events.view")?conteudos.eventos(equipe):List.of(),
            equipe!=null&&a.permissoes().contains("projects.view")?conteudos.projetos(equipe):List.of());
    }
    private void validar(PoliticaTarefa.Acesso acesso,Dados d,Registro anterior) {
        if(d==null||d.canal()==null||d.formato()==null||d.status()==null||d.equipeId()==null)throw new IllegalArgumentException();
        PoliticaTarefa.validarTitulo(d.titulo());PoliticaTarefa.validarDescricao(d.briefing());equipe(acesso,d.equipeId());
        if(d.publicacaoPlanejada()!=null&&(d.publicacaoPlanejada().getYear()<1000||d.publicacaoPlanejada().getYear()>9999))throw new IllegalArgumentException();
        if(d.responsavelId()!=null&&equipes.contarResponsaveisElegiveis(d.equipeId(),Set.of(d.responsavelId()))!=1)
            throw ProblemaDominio.conflito("Selecione uma responsável ativa da equipe do conteúdo.");
        // Vínculos históricos já existentes são preservados; novos vínculos exigem leitura da fonte e mesma equipe.
        boolean mesmoEvento=anterior!=null&&Objects.equals(anterior.eventoId(),d.eventoId())&&anterior.equipe().id().equals(d.equipeId());
        boolean mesmoProjeto=anterior!=null&&Objects.equals(anterior.projetoId(),d.projetoId())&&anterior.equipe().id().equals(d.equipeId());
        if(d.eventoId()!=null&&!mesmoEvento) {
            if(!acesso.permissoes().contains("events.view"))throw new AccessDeniedException("Acesso negado.");
            if(!conteudos.eventoAtivo(d.eventoId(),d.equipeId()))throw ProblemaDominio.conflito("Vincule um evento ativo da mesma equipe.");
        }
        if(d.projetoId()!=null&&!mesmoProjeto) {
            if(!acesso.permissoes().contains("projects.view"))throw new AccessDeniedException("Acesso negado.");
            if(!conteudos.projetoAtivo(d.projetoId(),d.equipeId()))throw ProblemaDominio.conflito("Vincule um projeto ativo da mesma equipe.");
        }
    }
    @Transactional
    public Resposta criar(String pessoa,Dados d) {
        bloqueio.adquirir();var a=acesso(pessoa,"content.create");if(a.perfil().equals("SUPPORT"))throw new AccessDeniedException("Acesso negado.");
        validar(a,d,null);var id=UUID.randomUUID().toString();conteudos.inserir(id,d,pessoa);
        auditoria.registrar(pessoa,"content.created","Content",id);return detalhe(pessoa,id);
    }
    private Registro alteravel(String pessoa,String id,long versao,boolean arquivo) {
        var a=acesso(pessoa,arquivo?"content.archive":"content.edit");var r=buscar(a,id);
        if(a.perfil().equals("SUPPORT"))throw new AccessDeniedException("Acesso negado.");
        if(r.arquivadaEm()!=null||r.equipeArquivada())throw ProblemaDominio.conflito("Conteúdo arquivado: somente leitura.");
        if(versao<0)throw new IllegalArgumentException();if(r.versao()!=versao)throw ProblemaDominio.conflito(CONFLITO);return r;
    }
    @Transactional
    public Resposta editar(String pessoa,String id,Dados d,long versao) {
        bloqueio.adquirir();var r=alteravel(pessoa,id,versao,false);validar(acesso(pessoa,"content.edit"),d,r);
        if(conteudos.atualizar(r,d)!=1)throw ProblemaDominio.conflito(CONFLITO);
        auditoria.registrar(pessoa,r.status()!=d.status()?"content.status_changed":"content.updated","Content",id);return detalhe(pessoa,id);
    }
    @Transactional
    public Resposta arquivar(String pessoa,String id,long versao) {
        bloqueio.adquirir();var r=alteravel(pessoa,id,versao,true);if(conteudos.arquivar(r)!=1)throw ProblemaDominio.conflito(CONFLITO);
        auditoria.registrar(pessoa,"content.archived","Content",id);return detalhe(pessoa,id);
    }
}
