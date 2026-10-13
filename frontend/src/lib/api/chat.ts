import {requisitarJava} from "./http";
export type CanalChat={id:string;nome:string;arquivado:boolean};
export type MensagemChat={id:string;autora:{id:string;nome:string};texto:string|null;versao:number;criadaEm:string;atualizadaEm:string;removida:boolean;capacidades:{editar:boolean;remover:boolean}};
export type PaginaChat={items:MensagemChat[];proximaPagina:string|null;podeEnviar:boolean;canal:CanalChat};
export const listarMensagens=(canal:string,antes?:string)=>requisitarJava<PaginaChat>(`/chat/channels/${canal}/messages?size=50${antes?`&before=${antes}`:""}`);
export const enviarMensagem=(canal:string,texto:string)=>requisitarJava<MensagemChat>(`/chat/channels/${canal}/messages`,"POST",{texto});
export const editarMensagem=(mensagem:MensagemChat,texto:string)=>requisitarJava<MensagemChat>(`/chat/messages/${mensagem.id}`,"PATCH",{texto,versao:mensagem.versao});
export const removerMensagem=(mensagem:MensagemChat)=>requisitarJava<MensagemChat>(`/chat/messages/${mensagem.id}/remove`,"POST",{versao:mensagem.versao});
