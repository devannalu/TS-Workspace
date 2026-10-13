import { exigirSessao } from "@/lib/sessao";
import { lerJava } from "@/lib/api/server";
import type { OpcoesParcerias } from "@/lib/api/parcerias";
import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { ErrorState } from "@/components/ui/feedback";
import { ListaParcerias } from "@/components/parcerias/lista-parcerias";
export const metadata={title:"Parcerias"};
export default async function ParceriasPage({searchParams}:{searchParams:Promise<Record<string,string|string[]|undefined>>}) {
 const usuario=await exigirSessao();
 if(!usuario.permissions.includes("partnerships.view"))return <ShellWorkspace><ErrorState message="Você não tem permissão para acessar esta área."/></ShellWorkspace>;
 const parametros=await searchParams;const opcoes=await lerJava<OpcoesParcerias>("/partnerships/options");
 return <ShellWorkspace><ListaParcerias opcoes={opcoes} podeCriar={usuario.permissions.includes("partnerships.create")&&usuario.role.key!=="SUPPORT"} abrirInicial={typeof parametros.abrir==="string"?parametros.abrir:undefined}/></ShellWorkspace>;
}
