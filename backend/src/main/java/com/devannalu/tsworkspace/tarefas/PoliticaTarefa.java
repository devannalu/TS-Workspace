package com.devannalu.tsworkspace.tarefas;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;

public final class PoliticaTarefa {
    private PoliticaTarefa() { }

    public record Acesso(String usuarioId, String perfil, Set<String> permissoes) {
        public boolean global() { return "SUPER_ADMIN".equals(perfil) || "ADMIN".equals(perfil); }
    }

    public static String validarTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty() || titulo.trim().length() > 200) throw new IllegalArgumentException();
        return titulo.trim();
    }

    public static String validarDescricao(String descricao) {
        if (descricao != null && descricao.length() > 5000) throw new IllegalArgumentException();
        return descricao == null || descricao.isBlank() ? null : descricao.trim();
    }

    public static Tarefa.Prioridade prioridadeInicial(Tarefa.Prioridade prioridade) {
        return prioridade == null ? Tarefa.Prioridade.MEDIA : prioridade;
    }

    public static boolean atrasada(LocalDate prazo, Tarefa.Status status, LocalDate hoje) {
        return prazo != null && prazo.isBefore(hoje) && status != Tarefa.Status.CONCLUIDA;
    }

    public static void exigirEquipe(Acesso acesso, boolean integrante) {
        if (!acesso.global() && !integrante) throw new AccessDeniedException("Acesso negado.");
    }

    public static boolean podeEditar(Acesso acesso, String criadoraId, boolean responsavel) {
        return acesso.permissoes().contains("tasks.edit") && (!"SUPPORT".equals(acesso.perfil())
            || acesso.usuarioId().equals(criadoraId) || responsavel);
    }

    public static void exigirVersao(long atual, Long informada) {
        if (informada == null || informada < 0) throw new IllegalArgumentException();
        if (atual != informada) throw ProblemaDominio.conflito(
            "Esta tarefa foi atualizada por outra pessoa. Atualize os dados e tente novamente.");
    }

    public static void exigirEquipeAtiva(boolean arquivada) {
        if (arquivada) throw ProblemaDominio.conflito("Equipe arquivada: tarefas históricas são somente leitura.");
    }
}
