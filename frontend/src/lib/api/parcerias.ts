import { requisitarJava } from "./http";
import type { ReferenciaTarefa } from "./tarefas";
export const statusParceria={PROSPECCAO:"Prospecção",CONTATO:"Contato",NEGOCIACAO:"Negociação",CONFIRMADA:"Confirmada",ENCERRADA:"Encerrada"} as const;
export type Parceria={id:string;organizacao:string;descricao:string|null;contatoPrincipal:string|null;email:string|null;telefone:string|null;proximaAcao:string|null;observacoes:string|null;status:keyof typeof statusParceria;equipe:ReferenciaTarefa;responsavel:ReferenciaTarefa|null;dataProximaAcao:string|null;eventoId:string|null;projetoId:string|null;criadaPor:ReferenciaTarefa;versao:number;criadaEm:string;atualizadaEm:string;arquivada:boolean;capacidades:{editar:boolean;arquivar:boolean;verEvento:boolean;verProjeto:boolean}};
export type DadosParceria=Pick<Parceria,"organizacao"|"contatoPrincipal"|"email"|"telefone"|"proximaAcao"|"observacoes"|"status"|"dataProximaAcao"|"eventoId"|"projetoId"> & {descricao:string;equipeId:string;responsavelId:string|null};
export type OpcoesParcerias={equipes:ReferenciaTarefa[];responsaveis:ReferenciaTarefa[];eventos:ReferenciaTarefa[];projetos:ReferenciaTarefa[]};
export type PaginaParcerias={items:Parceria[];total:number;page:number;size:number};
export function listarParcerias(busca="",equipe="",status="",arquivadas=false,pagina=0){const p=new URLSearchParams({search:busca,archived:String(arquivadas),page:String(pagina),size:"25"});if(equipe)p.set("teamId",equipe);if(status)p.set("status",status);return requisitarJava<PaginaParcerias>(`/partnerships?${p}`);}
export const buscarParceria=(id:string)=>requisitarJava<Parceria>(`/partnerships/${id}`);
export const opcoesParcerias=(equipe="")=>requisitarJava<OpcoesParcerias>(`/partnerships/options${equipe?`?teamId=${equipe}`:""}`);
export const criarParceria=(dados:DadosParceria)=>requisitarJava<Parceria>("/partnerships","POST",dados);
export const editarParceria=(parceria:Parceria,dados:DadosParceria)=>requisitarJava<Parceria>(`/partnerships/${parceria.id}`,"PATCH",{...dados,versao:parceria.versao});
export const arquivarParceria=(parceria:Parceria)=>requisitarJava<Parceria>(`/partnerships/${parceria.id}/archive`,"POST",{versao:parceria.versao});
