import {exigirSessao} from "@/lib/sessao";
import {lerJava} from "@/lib/api/server";
import type {CanalChat} from "@/lib/api/chat";
import {ShellWorkspace} from "@/components/layout/shell-workspace";
import {ErrorState} from "@/components/ui/feedback";
import {ConversaWorkspace} from "@/components/chat/conversa-workspace";
export const metadata={title:"Chat"};
export default async function ChatPage(){
  const pessoa=await exigirSessao();
  if(!pessoa.permissions.includes("chat.view"))return <ShellWorkspace><ErrorState message="Você não tem permissão para acessar esta área."/></ShellWorkspace>;
  const canais=await lerJava<CanalChat[]>("/chat/channels");
  return <ShellWorkspace><ConversaWorkspace canais={canais} pessoaId={pessoa.id}/></ShellWorkspace>;
}
