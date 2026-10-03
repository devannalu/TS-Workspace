package com.devannalu.tsworkspace.equipes;

import java.util.*;

// Preserva as regras de hierarquia e participação do checkpoint fabb672.
public final class PoliticaEquipe {
    private PoliticaEquipe() { }
    public record NoHierarquia(String id, String key, String parentId, boolean archived) { }
    public static void validarEquipeMae(List<NoHierarquia> nodes, String id, String parentId) {
        if (parentId == null) throw ProblemaEquipe.conflito("Não é permitido criar outra raiz pelo fluxo administrativo.");
        if (id.equals(parentId)) throw ProblemaEquipe.conflito("Uma equipe não pode ser sua própria equipe mãe.");
        Map<String, NoHierarquia> byId = new HashMap<>();
        nodes.forEach(n -> byId.put(n.id(), n));
        NoHierarquia current = byId.get(id);
        if (current != null && (current.parentId() == null || current.key().equals("fundadoras")))
            throw ProblemaEquipe.conflito("A equipe Fundadoras é estrutural e não pode ser movida.");
        if (current != null && current.archived()) throw ProblemaEquipe.conflito("Uma equipe arquivada não pode ser movida.");
        NoHierarquia cursor = byId.get(parentId);
        if (cursor == null) throw ProblemaEquipe.naoEncontrada("Equipe mãe inexistente.");
        if (cursor.archived()) throw ProblemaEquipe.conflito("Equipe mãe arquivada.");
        Set<String> visited = new HashSet<>(Set.of(id));
        while (cursor != null) {
            if (!visited.add(cursor.id())) throw ProblemaEquipe.conflito("A alteração criaria ou manteria um ciclo na hierarquia.");
            if (cursor.parentId() == null) break;
            cursor = byId.get(cursor.parentId());
            if (cursor == null) throw ProblemaEquipe.conflito("Hierarquia inválida: ancestral inexistente.");
        }
    }
    public static void arquivarEquipe(NoHierarquia team, long activeChildren) {
        if (team.parentId() == null || team.key().equals("fundadoras"))
            throw ProblemaEquipe.conflito("A equipe Fundadoras é estrutural e não pode ser arquivada.");
        if (!team.archived() && activeChildren > 0)
            throw ProblemaEquipe.conflito("Mova ou arquive as subequipes antes de arquivar esta equipe.");
    }
    public static void adicionarIntegrante(boolean active, boolean duplicate) {
        if (!active) throw ProblemaEquipe.conflito("Usuária indisponível.");
        if (duplicate) throw ProblemaEquipe.conflito("A integrante já está nesta equipe.");
    }
    public static void removerIntegrante(String teamKey, String role, long activeAdmins) {
        if (teamKey.equals("fundadoras") && "SUPER_ADMIN".equals(role) && activeAdmins <= 1)
            throw ProblemaEquipe.conflito("A última Super Admin não pode ser removida de Fundadoras.");
    }
}
