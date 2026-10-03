package com.devannalu.tsworkspace.usuarios;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;

public final class PoliticaUsuario {
    private PoliticaUsuario() { }
    public static void protegerAcessoAdministrativo(String atorId,String usuarioId,String perfilAcessoAnterior,boolean usuarioAnteriorAtivo,String proximoPerfilAcesso,boolean usuarioPermaneceraAtivo,long superAdminsAtivas) {
        boolean permaneceSuperAdmin="SUPER_ADMIN".equals(proximoPerfilAcesso)&&usuarioPermaneceraAtivo;
        // A regra histórica também protege SUPER_ADMIN inativa quando resta uma administradora.
        if("SUPER_ADMIN".equals(perfilAcessoAnterior)&&!permaneceSuperAdmin&&superAdminsAtivas<=1)
            throw ProblemaDominio.conflito("A última Super Admin ativa não pode perder o acesso administrativo.");
        if(atorId.equals(usuarioId)&&!permaneceSuperAdmin)
            throw ProblemaDominio.conflito("Você não pode remover seu próprio acesso administrativo.");
    }
    public static void protegerFundadoras(boolean eraSuperAdminAtivaFundadoras,boolean permaneceSuperAdminAtivaFundadoras,long outrasSuperAdminsAtivas) {
        if(eraSuperAdminAtivaFundadoras&&!permaneceSuperAdminAtivaFundadoras&&outrasSuperAdminsAtivas==0)
            throw ProblemaDominio.conflito("A última Super Admin ativa não pode ser removida de Fundadoras.");
    }
}
