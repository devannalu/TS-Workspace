import { requisitarJava } from "./http";
export type ItemChecklist = { id:string; texto:string; concluido:boolean; ordem:number; versao:number; criadaPor:{id:string;nome:string}; criadaEm:string; atualizadaEm:string };
export type ListaChecklist = { items:ItemChecklist[]; concluidos:number; total:number; podeEditar:boolean };
const rota=(tarefa:string)=>`/tasks/${tarefa}/checklist`;
export const listarChecklist=(tarefa:string)=>requisitarJava<ListaChecklist>(rota(tarefa));
export const criarItemChecklist=(tarefa:string,texto:string)=>requisitarJava<ListaChecklist>(rota(tarefa),"POST",{texto});
export const editarItemChecklist=(tarefa:string,item:ItemChecklist,texto:string,concluido:boolean)=>requisitarJava<ListaChecklist>(`${rota(tarefa)}/${item.id}`,"PATCH",{texto,concluido,versao:item.versao});
export const removerItemChecklist=(tarefa:string,item:ItemChecklist)=>requisitarJava<ListaChecklist>(`${rota(tarefa)}/${item.id}/remove`,"POST",{versao:item.versao});
export const ordenarChecklist=(tarefa:string,itens:ItemChecklist[])=>requisitarJava<ListaChecklist>(`${rota(tarefa)}/order`,"PATCH",{items:itens.map(i=>({id:i.id,versao:i.versao}))});
