import {exigirSessao} from "@/lib/sessao";
import {ShellWorkspace} from "@/components/layout/shell-workspace";
import {BuscaWorkspace} from "@/components/busca/busca-workspace";
export const metadata={title:"Busca global"};
export default async function BuscaPage({searchParams}:{searchParams:Promise<{q?:string}>}) {
  await exigirSessao();
  const parametros=await searchParams;
  const consulta=typeof parametros.q==="string"&&parametros.q.trim().length>=2&&parametros.q.length<=160?parametros.q:"";
  return <ShellWorkspace><BuscaWorkspace consultaInicial={consulta}/></ShellWorkspace>;
}
