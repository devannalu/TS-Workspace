package com.devannalu.tsworkspace.projetos;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;

public final class PoliticaProjeto {
    private PoliticaProjeto() { }
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
    public static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) throw new IllegalArgumentException();
    }
    public static void exigirEquipe(Acesso acesso, boolean integrante) {
        if (!acesso.global() && !integrante) throw new AccessDeniedException("Acesso negado.");
    }
    public static boolean podeAlterar(Acesso acesso, String permissao) {
        return !"SUPPORT".equals(acesso.perfil()) && acesso.permissoes().contains(permissao);
    }
    public static void exigirVersao(long atual, long informada) {
        if (informada < 0) throw new IllegalArgumentException();
        if (atual != informada) throw ProblemaDominio.conflito(
            "Este projeto foi atualizado por outra pessoa. Atualize os dados e tente novamente.");
    }
    public static void exigirSemPendencias(long total, long concluidas, boolean arquivar) {
        if (total > concluidas) throw ProblemaDominio.conflito(arquivar
            ? "Conclua ou arquive as tarefas pendentes antes de arquivar o projeto."
            : "Conclua ou arquive as tarefas pendentes antes de concluir o projeto.");
    }
    public static Integer percentual(long total, long concluidas) {
        return total == 0 ? null : (int) Math.round(100.0 * concluidas / total);
    }
}
