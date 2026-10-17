import {exigirSessao} from "@/lib/sessao";
import {lerJava} from "@/lib/api/server";
import type {OpcoesAuditoria} from "@/lib/api/auditoria";
import {ShellWorkspace} from "@/components/layout/shell-workspace";
import {ErrorState} from "@/components/ui/feedback";
import {AuditoriaWorkspace} from "@/components/auditoria/auditoria-workspace";
export const metadata={title:"Auditoria"};
export default async function AuditoriaPage(){
  const pessoa=await exigirSessao();
  if(!pessoa.permissions.includes("audit.view"))return <ShellWorkspace><ErrorState message="Você não tem permissão para acessar esta área."/></ShellWorkspace>;
  const opcoes=await lerJava<OpcoesAuditoria>("/audit/options");
  return <ShellWorkspace><AuditoriaWorkspace opcoes={opcoes}/></ShellWorkspace>;
}
