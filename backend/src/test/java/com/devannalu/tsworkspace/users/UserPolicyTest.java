package com.devannalu.tsworkspace.users;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class UserPolicyTest {
    @Test void lastAdminCannotBeDemoted() {
        assertThatThrownBy(()->UserPolicy.protect("actor","target","SUPER_ADMIN",true,"SUPPORT",true,1)).hasMessageContaining("última");
    }
    @Test void lastAdminCannotBeDeactivated() {
        assertThatThrownBy(()->UserPolicy.protect("actor","target","SUPER_ADMIN",true,"SUPER_ADMIN",false,1)).hasMessageContaining("última");
    }
    @Test void selfAdministrativeAccessCannotBeRemovedEvenWithAnotherAdmin() {
        assertThatThrownBy(()->UserPolicy.protect("self","self","SUPER_ADMIN",true,"ADMIN",true,2)).hasMessageContaining("próprio");
        assertThatThrownBy(()->UserPolicy.protect("self","self","ADMIN",true,"ADMIN",true,2)).hasMessageContaining("próprio");
    }
    @Test void anotherAdministratorAllowsRoleOrStatusChange() {
        assertThatCode(()->UserPolicy.protect("actor","target","SUPER_ADMIN",true,"SUPPORT",true,2)).doesNotThrowAnyException();
        assertThatCode(()->UserPolicy.protect("actor","target","SUPER_ADMIN",true,"SUPER_ADMIN",false,2)).doesNotThrowAnyException();
    }
    @Test void reactivationRestoresProfileAccessWithoutChangingIdentity() {
        assertThatCode(()->UserPolicy.protect("actor","target","SUPER_ADMIN",false,"SUPER_ADMIN",true,1)).doesNotThrowAnyException();
        assertThatCode(()->UserPolicy.protect("actor","target","SUPPORT",false,"SUPPORT",true,1)).doesNotThrowAnyException();
    }
    @Test void inactiveAdminMatchesLegacyGuard() {
        assertThatThrownBy(()->UserPolicy.protect("actor","target","SUPER_ADMIN",false,"SUPPORT",false,1)).hasMessageContaining("última");
    }
    @Test void foundersProtectionAppliesToMembershipRoleAndStatusLoss() {
        assertThatThrownBy(()->UserPolicy.founders(true,false,0)).hasMessageContaining("Fundadoras");
        assertThatCode(()->UserPolicy.founders(true,false,1)).doesNotThrowAnyException();
        assertThatCode(()->UserPolicy.founders(true,true,0)).doesNotThrowAnyException();
    }
}
