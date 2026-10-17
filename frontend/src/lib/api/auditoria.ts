import {requisitarJava} from "./http";
export type EventoAuditoria={id:string;autora:{id:string;nome:string}|null;acao:string;tipo:string;recursoId:string|null;ocorridoEm:string};
export type PaginaAuditoria={items:EventoAuditoria[];total:number;page:number;size:number;desde:string;ate:string};
export type OpcoesAuditoria={acoes:string[];tipos:string[]};
export type FiltrosAuditoria={autora:string;acao:string;tipo:string;desde:string;ate:string};
export const nomesRecursos:Record<string,string>={User:"Usuária",Invite:"Convite",Team:"Equipe",Task:"Tarefa",Project:"Projeto",Comment:"Comentário",Attachment:"Anexo",Meeting:"Reunião",Event:"Evento",Content:"Conteúdo",Partnership:"Parceria",Note:"Nota",ChatMessage:"Mensagem de chat",UNKNOWN:"Recurso não identificado"};
const fontes:Record<string,string>={user:"Usuária",invite:"Convite",team:"Equipe",task:"Tarefa",project:"Projeto",comment:"Comentário",attachment:"Anexo",meeting:"Reunião",event:"Evento",content:"Conteúdo",partnerships:"Parceria",note:"Nota",chat:"Chat"};
const verbos:Record<string,string>={created:"Criação",updated:"Edição",archived:"Arquivamento",removed:"Remoção",cancelled:"Cancelamento",accepted:"Aceite",role_changed:"Alteração de perfil",status_changed:"Alteração de status",teams_changed:"Alteração de equipes",assignees_changed:"Alteração de responsáveis",responsibles_changed:"Alteração de responsáveis",member_added:"Inclusão de integrante",member_removed:"Remoção de integrante",message_updated:"Edição de mensagem",message_removed:"Remoção de mensagem"};
export function nomeAcao(acao:string){const [fonte,verbo]=acao.split(".");return fontes[fonte]&&verbos[verbo]?`${verbos[verbo]} · ${fontes[fonte]}`:"Ação registrada";}
function inicioDia(valor:string,dias=0){const [ano,mes,dia]=valor.split("-").map(Number);return new Date(ano,mes-1,dia+dias).toISOString();}
export function listarAuditoria(filtros:FiltrosAuditoria,pagina=0){
  const parametros=new URLSearchParams({page:String(pagina),size:"25"});
  if(filtros.autora.trim())parametros.set("actor",filtros.autora.trim());
  if(filtros.acao)parametros.set("action",filtros.acao);
  if(filtros.tipo)parametros.set("resourceType",filtros.tipo);
  if(filtros.desde)parametros.set("from",inicioDia(filtros.desde));
  if(filtros.ate)parametros.set("to",inicioDia(filtros.ate,1));
  return requisitarJava<PaginaAuditoria>(`/audit?${parametros}`);
}
export const buscarEventoAuditoria=(id:string)=>requisitarJava<EventoAuditoria>(`/audit/${id}`);
