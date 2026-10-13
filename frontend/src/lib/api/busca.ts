import {requisitarJava} from "./http";
export const nomesBusca={TAREFA:"Tarefas",PROJETO:"Projetos",REUNIAO:"Reuniões",EVENTO:"Eventos",CONTEUDO:"Conteúdo",PARCERIA:"Parcerias",NOTA:"Notas"};
export type TipoBusca=keyof typeof nomesBusca;
export type ResultadoBusca={id:string;titulo:string;resumo:string|null;contexto:string;rota:string};
export type GrupoBusca={tipo:TipoBusca;items:ResultadoBusca[];total:number;page:number;size:number};
export type RespostaBusca={consulta:string;tipos:TipoBusca[];grupos:GrupoBusca[]};
export function buscarWorkspace(consulta:string,tipo?:TipoBusca,pagina=0){
  const parametros=new URLSearchParams({q:consulta,page:String(pagina),size:"20"});
  if(tipo)parametros.set("type",tipo);
  return requisitarJava<RespostaBusca>(`/search?${parametros}`);
}
