import { exigirSessao } from "@/lib/sessao";
import { lerJava } from "@/lib/api/server";
import type { OpcoesReunioes } from "@/lib/api/reunioes";
import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { ErrorState } from "@/components/ui/feedback";
import { ListaReunioes } from "@/components/reunioes/lista-reunioes";
export const metadata={title:"Reuniões e Talks"};
export default async function ReunioesPage({searchParams}:{searchParams:Promise<Record<string,string|string[]|undefined>>}) {
 const usuario=await exigirSessao();
 if(!usuario.permissions.includes("meetings.view"))return <ShellWorkspace><ErrorState message="Você não tem permissão para acessar esta área."/></ShellWorkspace>;
 const parametros=await searchParams;const opcoes=await lerJava<OpcoesReunioes>("/meetings/options");
 return <ShellWorkspace><ListaReunioes opcoes={opcoes} podeCriar={usuario.permissions.includes("meetings.create")&&usuario.role.key!=="SUPPORT"} abrirInicial={typeof parametros.abrir==="string"?parametros.abrir:undefined}/></ShellWorkspace>;
}
