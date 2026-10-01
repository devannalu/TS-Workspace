package com.devannalu.tsworkspace.teams;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TeamPolicyTest {
    private final List<TeamPolicy.Node> tree = List.of(
        new TeamPolicy.Node("root","fundadoras",null,false),
        new TeamPolicy.Node("a","a","root",false),
        new TeamPolicy.Node("b","b","a",false),
        new TeamPolicy.Node("c","c","b",false),
        new TeamPolicy.Node("archived","archived","root",true));
    @Test void allowsValidSubteamAndMove() {
        assertThatCode(() -> TeamPolicy.parent(tree,"new","a")).doesNotThrowAnyException();
        assertThatCode(() -> TeamPolicy.parent(tree,"c","root")).doesNotThrowAnyException();
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
        assertThatThrownBy(() -> TeamPolicy.parent(List.of(new TeamPolicy.Node("x","x","y",false),new TeamPolicy.Node("y","y","x",false)),"new","x")).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(() -> TeamPolicy.parent(List.of(new TeamPolicy.Node("x","x","missing",false)),"new","x")).isInstanceOf(TeamProblem.class);
    }
    @Test void foundersCannotArchiveAndChildrenMustBeMovedFirst() {
        assertThatThrownBy(() -> TeamPolicy.archive(tree.get(0),0)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(() -> TeamPolicy.archive(tree.get(1),1)).hasMessageContaining("subequipes");
        assertThatCode(() -> TeamPolicy.archive(tree.get(4),0)).doesNotThrowAnyException();
    }
    @Test void duplicateAndInactiveMembership() {
        assertThatThrownBy(() -> TeamPolicy.addMember(true,true)).hasMessageContaining("já");
        assertThatThrownBy(() -> TeamPolicy.addMember(false,false)).hasMessageContaining("indisponível");
        assertThatCode(() -> TeamPolicy.addMember(true,false)).doesNotThrowAnyException();
    }
    @Test void lastAdminMatchesLegacyIncludingInactiveTarget() {
        assertThatThrownBy(() -> TeamPolicy.removeMember("fundadoras","SUPER_ADMIN",1)).hasMessageContaining("última");
        assertThatThrownBy(() -> TeamPolicy.removeMember("fundadoras","SUPER_ADMIN",0)).isInstanceOf(TeamProblem.class);
        assertThatCode(() -> TeamPolicy.removeMember("fundadoras","SUPER_ADMIN",2)).doesNotThrowAnyException();
        assertThatCode(() -> TeamPolicy.removeMember("fundadoras","SUPPORT",1)).doesNotThrowAnyException();
        assertThatCode(() -> TeamPolicy.removeMember("other","SUPER_ADMIN",1)).doesNotThrowAnyException();
    }
    private void rejects(String id,String parent,int status) {
        assertThatThrownBy(() -> TeamPolicy.parent(tree,id,parent)).isInstanceOfSatisfying(TeamProblem.class,e -> assertThat(e.status()).isEqualTo(status));
    }
}
