package com.devannalu.tsworkspace.equipes;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PoliticaEquipeTest {
    private final List<PoliticaEquipe.NoHierarquia> listarHierarquia = List.of(
        new PoliticaEquipe.NoHierarquia("root","fundadoras",null,false),
        new PoliticaEquipe.NoHierarquia("a","a","root",false),
        new PoliticaEquipe.NoHierarquia("b","b","a",false),
        new PoliticaEquipe.NoHierarquia("c","c","b",false),
        new PoliticaEquipe.NoHierarquia("archived","archived","root",true));
    @Test void devePermitirSubequipeEMovimentacaoValidas() {
        assertThatCode(() -> PoliticaEquipe.validarEquipeMae(listarHierarquia,"new","a")).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaEquipe.validarEquipeMae(listarHierarquia,"c","root")).doesNotThrowAnyException();
    }
    @Test void deveRejeitarEquipeMaeIgualAPropriaEquipe() { rejects("a","a",409); }
    @Test void deveRejeitarCicloDireto() { rejects("a","b",409); }
    @Test void deveRejeitarCicloComDescendenteIndireta() { rejects("a","c",409); }
    @Test void deveRejeitarEquipeMaeInexistente() { rejects("a","missing",404); }
    @Test void deveRejeitarEquipeMaeArquivada() { rejects("a","archived",409); }
    @Test void deveRejeitarSegundaRaiz() { rejects("new",null,409); }
    @Test void naoDeveMoverFundadoras() { rejects("root","a",409); }
    @Test void naoDeveMoverEquipeArquivada() { rejects("archived","a",409); }
    @Test void deveRejeitarCicloExistenteEAncestralInexistente() {
        assertThatThrownBy(() -> PoliticaEquipe.validarEquipeMae(List.of(new PoliticaEquipe.NoHierarquia("x","x","y",false),new PoliticaEquipe.NoHierarquia("y","y","x",false)),"new","x")).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(() -> PoliticaEquipe.validarEquipeMae(List.of(new PoliticaEquipe.NoHierarquia("x","x","missing",false)),"new","x")).isInstanceOf(ProblemaDominio.class);
    }
    @Test void devePreservarFundadorasEExigirTratamentoDasSubequipes() {
        assertThatThrownBy(() -> PoliticaEquipe.arquivarEquipe(listarHierarquia.get(0),0)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(() -> PoliticaEquipe.arquivarEquipe(listarHierarquia.get(1),1)).hasMessageContaining("subequipes");
        assertThatCode(() -> PoliticaEquipe.arquivarEquipe(listarHierarquia.get(4),0)).doesNotThrowAnyException();
    }
    @Test void deveRejeitarIntegranteDuplicadaOuInativa() {
        assertThatThrownBy(() -> PoliticaEquipe.adicionarIntegrante(true,true)).hasMessageContaining("já");
        assertThatThrownBy(() -> PoliticaEquipe.adicionarIntegrante(false,false)).hasMessageContaining("indisponível");
        assertThatCode(() -> PoliticaEquipe.adicionarIntegrante(true,false)).doesNotThrowAnyException();
    }
    @Test void devePreservarRegraDaUltimaAdminInclusiveParaAlvoInativo() {
        assertThatThrownBy(() -> PoliticaEquipe.removerIntegrante("fundadoras","SUPER_ADMIN",1)).hasMessageContaining("última");
        assertThatThrownBy(() -> PoliticaEquipe.removerIntegrante("fundadoras","SUPER_ADMIN",0)).isInstanceOf(ProblemaDominio.class);
        assertThatCode(() -> PoliticaEquipe.removerIntegrante("fundadoras","SUPER_ADMIN",2)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaEquipe.removerIntegrante("fundadoras","SUPPORT",1)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaEquipe.removerIntegrante("other","SUPER_ADMIN",1)).doesNotThrowAnyException();
    }
    private void rejects(String id,String validarEquipeMae,int status) {
        assertThatThrownBy(() -> PoliticaEquipe.validarEquipeMae(listarHierarquia,id,validarEquipeMae)).isInstanceOfSatisfying(ProblemaDominio.class,e -> assertThat(e.status()).isEqualTo(status));
    }
}
