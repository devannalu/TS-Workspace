package com.devannalu.tsworkspace.usuarios;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PoliticaUsuarioTest {
    @Test void naoDeveRebaixarUltimaSuperAdmin() {
        assertThatThrownBy(()->PoliticaUsuario.protegerAcessoAdministrativo("actor","target","SUPER_ADMIN",true,"SUPPORT",true,1)).hasMessageContaining("última");
    }
    @Test void naoDeveInativarUltimaSuperAdmin() {
        assertThatThrownBy(()->PoliticaUsuario.protegerAcessoAdministrativo("actor","target","SUPER_ADMIN",true,"SUPER_ADMIN",false,1)).hasMessageContaining("última");
    }
    @Test void naoDeveRemoverAcessoAdministrativoProprio() {
        assertThatThrownBy(()->PoliticaUsuario.protegerAcessoAdministrativo("self","self","SUPER_ADMIN",true,"ADMIN",true,2)).hasMessageContaining("próprio");
        assertThatThrownBy(()->PoliticaUsuario.protegerAcessoAdministrativo("self","self","ADMIN",true,"ADMIN",true,2)).hasMessageContaining("próprio");
    }
    @Test void devePermitirAlteracaoComOutraSuperAdminAtiva() {
        assertThatCode(()->PoliticaUsuario.protegerAcessoAdministrativo("actor","target","SUPER_ADMIN",true,"SUPPORT",true,2)).doesNotThrowAnyException();
        assertThatCode(()->PoliticaUsuario.protegerAcessoAdministrativo("actor","target","SUPER_ADMIN",true,"SUPER_ADMIN",false,2)).doesNotThrowAnyException();
    }
    @Test void deveReativarPerfilSemAlterarIdentidade() {
        assertThatCode(()->PoliticaUsuario.protegerAcessoAdministrativo("actor","target","SUPER_ADMIN",false,"SUPER_ADMIN",true,1)).doesNotThrowAnyException();
        assertThatCode(()->PoliticaUsuario.protegerAcessoAdministrativo("actor","target","SUPPORT",false,"SUPPORT",true,1)).doesNotThrowAnyException();
    }
    @Test void devePreservarProtecaoHistoricaDaAdminInativa() {
        assertThatThrownBy(()->PoliticaUsuario.protegerAcessoAdministrativo("actor","target","SUPER_ADMIN",false,"SUPPORT",false,1)).hasMessageContaining("última");
    }
    @Test void deveProtegerFundadorasAoAlterarEquipePerfilOuSituacao() {
        assertThatThrownBy(()->PoliticaUsuario.protegerFundadoras(true,false,0)).hasMessageContaining("Fundadoras");
        assertThatCode(()->PoliticaUsuario.protegerFundadoras(true,false,1)).doesNotThrowAnyException();
        assertThatCode(()->PoliticaUsuario.protegerFundadoras(true,true,0)).doesNotThrowAnyException();
    }
}
