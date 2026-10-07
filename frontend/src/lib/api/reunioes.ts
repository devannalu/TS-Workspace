import { requisitarJava } from "./http";
import type { ReferenciaTarefa } from "./tarefas";
export const statusReuniao={AGENDADA:"Agendada",REALIZADA:"Realizada",CANCELADA:"Cancelada"} as const;
export type StatusReuniao=keyof typeof statusReuniao;
export type Reuniao={id:string;titulo:string;pauta:string|null;tipo:"REUNIAO"|"TALK";status:StatusReuniao;equipe:ReferenciaTarefa;inicio:string;fim:string;zona:string;inicioLocal:string;fimLocal:string;local:string|null;link:string|null;resultados:string|null;participantes:{id:string;nome:string;responsavel:boolean}[];criadaPor:ReferenciaTarefa;versao:number;criadaEm:string;atualizadaEm:string;arquivada:boolean;capacidades:{editar:boolean;arquivar:boolean}};
export type DadosReuniao={titulo:string;pauta:string;tipo:Reuniao["tipo"];status:StatusReuniao;equipeId:string;inicioLocal:string;fimLocal:string;zona:string;local:string;link:string;resultados:string;participanteIds:string[];responsavelIds:string[]};
export type OpcoesReunioes={equipes:ReferenciaTarefa[];participantes:ReferenciaTarefa[]};
export type PaginaReunioes={items:Reuniao[];total:number;page:number;size:number};
export function listarReunioes(busca="",equipe="",status="",arquivadas=false,pagina=0){const p=new URLSearchParams({search:busca,archived:String(arquivadas),page:String(pagina),size:"25"});if(equipe)p.set("teamId",equipe);if(status)p.set("status",status);return requisitarJava<PaginaReunioes>(`/meetings?${p}`);}
export const buscarReuniao=(id:string)=>requisitarJava<Reuniao>(`/meetings/${id}`);
export const opcoesReunioes=(equipe="")=>requisitarJava<OpcoesReunioes>(`/meetings/options${equipe?`?teamId=${equipe}`:""}`);
export const criarReuniao=(dados:DadosReuniao)=>requisitarJava<Reuniao>("/meetings","POST",dados);
export const editarReuniao=(reuniao:Reuniao,dados:DadosReuniao)=>requisitarJava<Reuniao>(`/meetings/${reuniao.id}`,"PATCH",{...dados,versao:reuniao.versao});
export const arquivarReuniao=(reuniao:Reuniao)=>requisitarJava<Reuniao>(`/meetings/${reuniao.id}/archive`,"POST",{versao:reuniao.versao});
export function formatarHorarioReuniao(instante:string,zona:string){return new Intl.DateTimeFormat("pt-BR",{dateStyle:"short",timeStyle:"short",timeZone:zona}).format(new Date(instante));}
