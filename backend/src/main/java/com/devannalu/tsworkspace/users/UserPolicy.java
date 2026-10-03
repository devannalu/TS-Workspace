package com.devannalu.tsworkspace.users;

import com.devannalu.tsworkspace.common.DomainProblem;

public final class UserPolicy {
    private UserPolicy() { }
    public static void protect(String actor,String target,String beforeRole,boolean beforeActive,String nextRole,boolean nextActive,long activeAdmins) {
        boolean remainsAdmin="SUPER_ADMIN".equals(nextRole)&&nextActive;
        // Baseline also protects inactive SUPER_ADMIN targets when count <= 1.
        if("SUPER_ADMIN".equals(beforeRole)&&!remainsAdmin&&activeAdmins<=1)
            throw DomainProblem.conflict("A última Super Admin ativa não pode perder o acesso administrativo.");
        if(actor.equals(target)&&!remainsAdmin)
            throw DomainProblem.conflict("Você não pode remover seu próprio acesso administrativo.");
    }
    public static void founders(boolean wasActiveAdminMember,boolean remainsActiveAdminMember,long otherActiveAdmins) {
        if(wasActiveAdminMember&&!remainsActiveAdminMember&&otherActiveAdmins==0)
            throw DomainProblem.conflict("A última Super Admin ativa não pode ser removida de Fundadoras.");
    }
}
