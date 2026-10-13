import { requisitarJava } from "./http";
export type ContextoNota="projects"|"events"|"partnerships"|"meetings";
export type Nota={id:string;autora:{id:string;nome:string};titulo:string;texto:string;versao:number;criadaEm:string;atualizadaEm:string;editar:boolean;remover:boolean};
export type PaginaNotas={items:Nota[];total:number;page:number;size:number;podeCriar:boolean};
export const listarNotas=(recurso:ContextoNota,id:string,pagina=0)=>requisitarJava<PaginaNotas>(`/${recurso}/${id}/notes?page=${pagina}&size=10`);
export const criarNota=(recurso:ContextoNota,id:string,titulo:string,texto:string)=>requisitarJava<Nota>(`/${recurso}/${id}/notes`,"POST",{titulo,texto});
export const editarNota=(nota:Nota,titulo:string,texto:string)=>requisitarJava<Nota>(`/notes/${nota.id}`,"PATCH",{titulo,texto,versao:nota.versao});
export const removerNota=(nota:Nota)=>requisitarJava<void>(`/notes/${nota.id}/remove`,"POST",{versao:nota.versao});

export const buscarNota=(recurso:ContextoNota,id:string,notaId:string)=>requisitarJava<Nota>(`/${recurso}/${id}/notes/${notaId}`);
