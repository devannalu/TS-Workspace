import { requisitarJava } from "./http";
export type Notificacao = { id:string; tipo:"TAREFA"|"PROJETO"; recursoId:string; titulo:string; motivo:string; criadaEm:string; lida:boolean };
export type PaginaNotificacoes = { items:Notificacao[]; total:number; naoLidas:number; page:number; size:number };
export const listarNotificacoes = (pagina=0) => requisitarJava<PaginaNotificacoes>(`/notifications?page=${pagina}&size=20`);
export const lerNotificacao = (id:string) => requisitarJava<void>(`/notifications/${id}/read`,"POST");
export const lerTodasNotificacoes = () => requisitarJava<void>("/notifications/read-all","POST");
