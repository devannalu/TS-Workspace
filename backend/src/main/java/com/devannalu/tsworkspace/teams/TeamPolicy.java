package com.devannalu.tsworkspace.teams;

import java.util.*;

/** Hierarchy and membership invariants validated against baseline fabb672. */
public final class TeamPolicy {
    private TeamPolicy() { }
    public record Node(String id, String key, String parentId, boolean archived) { }
    public static void parent(List<Node> nodes, String id, String parentId) {
        if (parentId == null) throw TeamProblem.conflict("Não é permitido criar outra raiz pelo fluxo administrativo.");
        if (id.equals(parentId)) throw TeamProblem.conflict("Uma equipe não pode ser sua própria equipe mãe.");
        Map<String, Node> byId = new HashMap<>();
        nodes.forEach(n -> byId.put(n.id(), n));
        Node current = byId.get(id);
        if (current != null && (current.parentId() == null || current.key().equals("fundadoras")))
            throw TeamProblem.conflict("A equipe Fundadoras é estrutural e não pode ser movida.");
        if (current != null && current.archived()) throw TeamProblem.conflict("Uma equipe arquivada não pode ser movida.");
        Node cursor = byId.get(parentId);
        if (cursor == null) throw TeamProblem.missing("Equipe mãe inexistente.");
        if (cursor.archived()) throw TeamProblem.conflict("Equipe mãe arquivada.");
        Set<String> visited = new HashSet<>(Set.of(id));
        while (cursor != null) {
            if (!visited.add(cursor.id())) throw TeamProblem.conflict("A alteração criaria ou manteria um ciclo na hierarquia.");
            if (cursor.parentId() == null) break;
            cursor = byId.get(cursor.parentId());
            if (cursor == null) throw TeamProblem.conflict("Hierarquia inválida: ancestral inexistente.");
        }
    }
    public static void archive(Node team, long activeChildren) {
        if (team.parentId() == null || team.key().equals("fundadoras"))
            throw TeamProblem.conflict("A equipe Fundadoras é estrutural e não pode ser arquivada.");
        if (!team.archived() && activeChildren > 0)
            throw TeamProblem.conflict("Mova ou arquive as subequipes antes de arquivar esta equipe.");
    }
    public static void addMember(boolean active, boolean duplicate) {
        if (!active) throw TeamProblem.conflict("Usuária indisponível.");
        if (duplicate) throw TeamProblem.conflict("A integrante já está nesta equipe.");
    }
    public static void removeMember(String teamKey, String role, long activeAdmins) {
        if (teamKey.equals("fundadoras") && "SUPER_ADMIN".equals(role) && activeAdmins <= 1)
            throw TeamProblem.conflict("A última Super Admin não pode ser removida de Fundadoras.");
    }
}
