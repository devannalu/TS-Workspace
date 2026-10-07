package com.devannalu.tsworkspace.comentarios;

import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import com.devannalu.tsworkspace.compartilhado.BloqueioOrganizacao;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.projetos.ProjetoService;
import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.tarefas.TarefaRepository;
import com.devannalu.tsworkspace.tarefas.TarefaService;
import com.devannalu.tsworkspace.comentarios.ComentarioRepository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComentarioService {
    public static final String CONFLITO = "Este comentário foi alterado. Atualize os dados e tente novamente.";
    private final ComentarioRepository comentarios;
    private final TarefaService tarefas;
    private final ProjetoService projetos;
    private final TarefaRepository equipes;
    private final PermissaoService permissoes;
    private final AuditoriaRepository auditoria;
    private final com.devannalu.tsworkspace.notificacoes.NotificacaoService notificacoes;
    private final BloqueioOrganizacao bloqueio;
    public ComentarioService(ComentarioRepository comentarios, TarefaService tarefas, ProjetoService projetos,
        TarefaRepository equipes, PermissaoService permissoes, AuditoriaRepository auditoria, BloqueioOrganizacao bloqueio, com.devannalu.tsworkspace.notificacoes.NotificacaoService notificacoes) {
        this.comentarios = comentarios; this.tarefas = tarefas; this.projetos = projetos; this.equipes = equipes;
        this.permissoes = permissoes; this.auditoria = auditoria; this.bloqueio = bloqueio; this.notificacoes = notificacoes;
    }
    public record Capacidades(boolean editar, boolean remover) { }
    public record ComentarioResponse(String id, Pessoa autora, String conteudo, boolean editado, boolean removido,
        long versao, Instant criadaEm, Instant atualizadaEm, Capacidades capacidades) { }
    public record PaginaComentarios(List<ComentarioResponse> items, long total, int page, int size, boolean podeComentar) { }
    public record AtividadeResponse(String id, String tipo, Pessoa ator, Instant criadaEm) { }
    public record PaginaAtividade(List<AtividadeResponse> items, long total, int page, int size) { }
    private record Acesso(boolean podeComentar, boolean podeModerar) { }
    private Acesso acesso(String usuarioId, Recurso recurso, String id) {
        boolean arquivado; String equipeId;
        if (recurso == Recurso.TAREFA) {
            var tarefa = tarefas.buscarTarefa(usuarioId, id);
            arquivado = tarefa.arquivada(); equipeId = tarefa.equipe().id();
        } else {
            var projeto = projetos.buscarProjeto(usuarioId, id);
            arquivado = projeto.arquivado(); equipeId = projeto.equipe().id();
        }
        boolean somenteLeitura = arquivado || equipes.equipeArquivada(equipeId).orElseThrow();
        var atuais = permissoes.buscarPermissoesUsuario(usuarioId);
        String perfil = atuais.role().key(); var chaves = atuais.chavesEfetivas();
        boolean comentar = chaves.contains(recurso.rota + ".comment")
            && !(recurso == Recurso.PROJETO && perfil.equals("SUPPORT"));
        boolean moderar = chaves.contains("comments.moderate")
            && (!somenteLeitura || perfil.equals("ADMIN") || perfil.equals("SUPER_ADMIN"))
            && !(recurso == Recurso.PROJETO && perfil.equals("SUPPORT"));
        return new Acesso(comentar && !somenteLeitura, moderar);
    }
    private void validarPagina(int pagina, int tamanho) {
        if (pagina < 0 || pagina > 100000 || tamanho < 1 || tamanho > 100) throw new IllegalArgumentException();
    }
    private String validarConteudo(String conteudo) {
        if (conteudo == null || conteudo.trim().isEmpty() || conteudo.length() > 5000) throw new IllegalArgumentException();
        return conteudo.trim();
    }
    private EstadoComentario buscar(String id) {
        return comentarios.buscar(id).orElseThrow(() -> ProblemaDominio.naoEncontrado("Comentário não encontrado."));
    }
    private ComentarioResponse resposta(String usuarioId, Acesso acesso, EstadoComentario c) {
        boolean removido = c.removidaEm() != null;
        boolean propria = c.autora().id().equals(usuarioId) && acesso.podeComentar();
        return new ComentarioResponse(c.id(), c.autora(), removido ? null : c.conteudo(),
            !removido && c.versao() > 0, removido, c.versao(), c.criadaEm(), c.atualizadaEm(),
            new Capacidades(!removido && propria, !removido && (propria || acesso.podeModerar())));
    }
    @Transactional(readOnly = true)
    public PaginaComentarios listar(String usuarioId, Recurso recurso, String id, int pagina, int tamanho) {
        validarPagina(pagina, tamanho); var acesso = acesso(usuarioId, recurso, id);
        return new PaginaComentarios(comentarios.listar(recurso, id, pagina, tamanho).stream()
            .map(c -> resposta(usuarioId, acesso, c)).toList(), comentarios.contar(recurso, id), pagina, tamanho, acesso.podeComentar());
    }
    @Transactional
    public ComentarioResponse criar(String usuarioId, Recurso recurso, String recursoId, String conteudo) {
        bloqueio.adquirir(); var acesso = acesso(usuarioId, recurso, recursoId);
        if (!acesso.podeComentar()) throw new AccessDeniedException("Acesso negado.");
        String id = UUID.randomUUID().toString();
        comentarios.inserir(id, recurso, recursoId, usuarioId, validarConteudo(conteudo));
        auditoria.registrar(usuarioId, "comment.created", "Comment", id);
        notificacoes.avisarResponsaveis(usuarioId,recurso.name(),recursoId,"COMENTARIO");
        return resposta(usuarioId, acesso, buscar(id));
    }
    private void exigirVersao(EstadoComentario c, long versao) {
        if (versao < 0) throw new IllegalArgumentException();
        if (c.versao() != versao || c.removidaEm() != null) throw ProblemaDominio.conflito(CONFLITO);
    }
    @Transactional
    public ComentarioResponse editar(String usuarioId, String id, String conteudo, long versao) {
        bloqueio.adquirir(); var c = buscar(id); var acesso = acesso(usuarioId, c.recurso(), c.recursoId());
        if (!c.autora().id().equals(usuarioId) || !acesso.podeComentar()) throw new AccessDeniedException("Acesso negado.");
        exigirVersao(c, versao);
        if (comentarios.editar(id, versao, validarConteudo(conteudo)) != 1) throw ProblemaDominio.conflito(CONFLITO);
        auditoria.registrar(usuarioId, "comment.updated", "Comment", id);
        return resposta(usuarioId, acesso, buscar(id));
    }
    @Transactional
    public ComentarioResponse remover(String usuarioId, String id, long versao) {
        bloqueio.adquirir(); var c = buscar(id); var acesso = acesso(usuarioId, c.recurso(), c.recursoId());
        if (!(c.autora().id().equals(usuarioId) && acesso.podeComentar()) && !acesso.podeModerar())
            throw new AccessDeniedException("Acesso negado.");
        exigirVersao(c, versao);
        if (comentarios.remover(id, versao, usuarioId) != 1) throw ProblemaDominio.conflito(CONFLITO);
        auditoria.registrar(usuarioId, "comment.removed", "Comment", id);
        return resposta(usuarioId, acesso, buscar(id));
    }
    @Transactional(readOnly = true)
    public PaginaAtividade listarAtividade(String usuarioId, Recurso recurso, String id, int pagina, int tamanho) {
        validarPagina(pagina, tamanho); acesso(usuarioId, recurso, id);
        return new PaginaAtividade(comentarios.listarAtividade(recurso, id, pagina, tamanho).stream()
            .map(e -> new AtividadeResponse(e.id(), e.tipo(), e.ator(), e.criadaEm())).toList(),
            comentarios.contarAtividade(recurso, id), pagina, tamanho);
    }
}
