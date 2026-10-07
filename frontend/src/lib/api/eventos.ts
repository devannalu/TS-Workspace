import { requisitarJava } from "./http";
import type { ReferenciaTarefa } from "./tarefas";
export const statusEvento={PLANEJADO:"Planejado",CONFIRMADO:"Confirmado",EM_ANDAMENTO:"Em andamento",CONCLUIDO:"Concluído",CANCELADO:"Cancelado"} as const;
export type StatusEvento=keyof typeof statusEvento;
export type Evento={id:string;nome:string;descricao:string|null;formato:"PRESENCIAL"|"ONLINE"|"HIBRIDO";status:StatusEvento;equipe:ReferenciaTarefa;inicio:string;fim:string;zona:string;inicioLocal:string;fimLocal:string;local:string|null;link:string|null;notas:string|null;responsaveis:ReferenciaTarefa[];criadaPor:ReferenciaTarefa;versao:number;criadaEm:string;atualizadaEm:string;arquivada:boolean;capacidades:{editar:boolean;arquivar:boolean}};
export type DadosEvento={nome:string;descricao:string;formato:Evento["formato"];status:StatusEvento;equipeId:string;inicioLocal:string;fimLocal:string;zona:string;local:string;link:string;notas:string;responsavelIds:string[]};
export type OpcoesEventos={equipes:ReferenciaTarefa[];responsaveis:ReferenciaTarefa[]};
export type PaginaEventos={items:Evento[];total:number;page:number;size:number};
export function listarEventos(busca="",equipe="",status="",arquivadas=false,pagina=0){const p=new URLSearchParams({search:busca,archived:String(arquivadas),page:String(pagina),size:"25"});if(equipe)p.set("teamId",equipe);if(status)p.set("status",status);return requisitarJava<PaginaEventos>(`/events?${p}`);}
export const buscarEvento=(id:string)=>requisitarJava<Evento>(`/events/${id}`);
export const opcoesEventos=(equipe="")=>requisitarJava<OpcoesEventos>(`/events/options${equipe?`?teamId=${equipe}`:""}`);
export const criarEvento=(dados:DadosEvento)=>requisitarJava<Evento>("/events","POST",dados);
export const editarEvento=(evento:Evento,dados:DadosEvento)=>requisitarJava<Evento>(`/events/${evento.id}`,"PATCH",{...dados,versao:evento.versao});
export const arquivarEvento=(evento:Evento)=>requisitarJava<Evento>(`/events/${evento.id}/archive`,"POST",{versao:evento.versao});
export function formatarHorarioEvento(instante:string,zona:string){return new Intl.DateTimeFormat("pt-BR",{dateStyle:"short",timeStyle:"short",timeZone:zona}).format(new Date(instante));}
