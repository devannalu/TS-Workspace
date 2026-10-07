import { exigirSessao } from "@/lib/sessao";
import { lerJava } from "@/lib/api/server";
import type { OpcoesEventos } from "@/lib/api/eventos";
import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { ErrorState } from "@/components/ui/feedback";
import { ListaEventos } from "@/components/eventos/lista-eventos";
export const metadata={title:"Eventos"};
export default async function EventosPage({searchParams}:{searchParams:Promise<Record<string,string|string[]|undefined>>}) {
 const usuario=await exigirSessao();
 if(!usuario.permissions.includes("events.view"))return <ShellWorkspace><ErrorState message="Você não tem permissão para acessar esta área."/></ShellWorkspace>;
 const parametros=await searchParams;const opcoes=await lerJava<OpcoesEventos>("/events/options");
 return <ShellWorkspace><ListaEventos opcoes={opcoes} podeCriar={usuario.permissions.includes("events.create")&&usuario.role.key!=="SUPPORT"} abrirInicial={typeof parametros.abrir==="string"?parametros.abrir:undefined}/></ShellWorkspace>;
}
