import { exigirSessao } from "@/lib/sessao";
import { lerJava } from "@/lib/api/server";
import type { OpcoesConteudos } from "@/lib/api/conteudos";
import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { ErrorState } from "@/components/ui/feedback";
import { ListaConteudos } from "@/components/conteudos/lista-conteudos";
export const metadata={title:"Comunicação / Conteúdo"};
export default async function ConteudosPage({searchParams}:{searchParams:Promise<Record<string,string|string[]|undefined>>}) {
 const usuario=await exigirSessao();
 if(!usuario.permissions.includes("content.view"))return <ShellWorkspace><ErrorState message="Você não tem permissão para acessar esta área."/></ShellWorkspace>;
 const parametros=await searchParams;const opcoes=await lerJava<OpcoesConteudos>("/content/options");
 return <ShellWorkspace><ListaConteudos opcoes={opcoes} podeCriar={usuario.permissions.includes("content.create")&&usuario.role.key!=="SUPPORT"} abrirInicial={typeof parametros.abrir==="string"?parametros.abrir:undefined}/></ShellWorkspace>;
}
