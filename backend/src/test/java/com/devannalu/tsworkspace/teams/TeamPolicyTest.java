package com.devannalu.tsworkspace.equipes;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TeamPolicyTest {
    private final List<PoliticaEquipe.NoHierarquia> listarHierarquia = List.of(
        new PoliticaEquipe.NoHierarquia("root","fundadoras",null,false),
        new PoliticaEquipe.NoHierarquia("a","a","root",false),
        new PoliticaEquipe.NoHierarquia("b","b","a",false),
        new PoliticaEquipe.NoHierarquia("c","c","b",false),
        new PoliticaEquipe.NoHierarquia("archived","archived","root",true));
    @Test void allowsValidSubteamAndMove() {
        assertThatCode(() -> PoliticaEquipe.validarEquipeMae(listarHierarquia,"new","a")).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaEquipe.validarEquipeMae(listarHierarquia,"c","root")).doesNotThrowAnyException();
    }
    @Test void selfParent() { rejects("a","a",409); }
    @Test void directCycle() { rejects("a","b",409); }
    @Test void indirectDescendantCycle() { rejects("a","c",409); }
    @Test void missingParent() { rejects("a","missing",404); }
    @Test void archivedParent() { rejects("a","archived",409); }
    @Test void secondRoot() { rejects("new",null,409); }
    @Test void foundersCannotMove() { rejects("root","a",409); }
    @Test void archivedCannotMove() { rejects("archived","a",409); }
    @Test void existingCycleAndBrokenAncestor() {
        assertThatThrownBy(() -> PoliticaEquipe.validarEquipeMae(List.of(new PoliticaEquipe.NoHierarquia("x","x","y",false),new PoliticaEquipe.NoHierarquia("y","y","x",false)),"new","x")).isInstanceOf(ProblemaEquipe.class);
        assertThatThrownBy(() -> PoliticaEquipe.validarEquipeMae(List.of(new PoliticaEquipe.NoHierarquia("x","x","missing",false)),"new","x")).isInstanceOf(ProblemaEquipe.class);
    }
    @Test void foundersCannotArchiveAndChildrenMustBeMovedFirst() {
        assertThatThrownBy(() -> PoliticaEquipe.arquivarEquipe(listarHierarquia.get(0),0)).isInstanceOf(ProblemaEquipe.class);
        assertThatThrownBy(() -> PoliticaEquipe.arquivarEquipe(listarHierarquia.get(1),1)).hasMessageContaining("subequipes");
        assertThatCode(() -> PoliticaEquipe.arquivarEquipe(listarHierarquia.get(4),0)).doesNotThrowAnyException();
    }
    @Test void duplicateAndInactiveMembership() {
        assertThatThrownBy(() -> PoliticaEquipe.adicionarIntegrante(true,true)).hasMessageContaining("já");
        assertThatThrownBy(() -> PoliticaEquipe.adicionarIntegrante(false,false)).hasMessageContaining("indisponível");
        assertThatCode(() -> PoliticaEquipe.adicionarIntegrante(true,false)).doesNotThrowAnyException();
    }
    @Test void lastAdminMatchesLegacyIncludingInactiveTarget() {
        assertThatThrownBy(() -> PoliticaEquipe.removerIntegrante("fundadoras","SUPER_ADMIN",1)).hasMessageContaining("última");
        assertThatThrownBy(() -> PoliticaEquipe.removerIntegrante("fundadoras","SUPER_ADMIN",0)).isInstanceOf(ProblemaEquipe.class);
        assertThatCode(() -> PoliticaEquipe.removerIntegrante("fundadoras","SUPER_ADMIN",2)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaEquipe.removerIntegrante("fundadoras","SUPPORT",1)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaEquipe.removerIntegrante("other","SUPER_ADMIN",1)).doesNotThrowAnyException();
    }
    private void rejects(String id,String validarEquipeMae,int status) {
        assertThatThrownBy(() -> PoliticaEquipe.validarEquipeMae(listarHierarquia,id,validarEquipeMae)).isInstanceOfSatisfying(ProblemaEquipe.class,e -> assertThat(e.status()).isEqualTo(status));
    }
}
