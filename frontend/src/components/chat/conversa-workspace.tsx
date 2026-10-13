"use client";
import {useEffect,useRef,useState,type FormEvent} from "react";
import {listarMensagens,enviarMensagem,editarMensagem,removerMensagem,type CanalChat,type MensagemChat,type PaginaChat} from "@/lib/api/chat";
import {ErroApi} from "@/lib/api/http";
import {Button} from "../ui/button";
import {ErrorState,EmptyState} from "../ui/feedback";

export function ConversaWorkspace({canais,pessoaId}:{canais:CanalChat[];pessoaId:string}) {
  const [canal,definirCanal]=useState("geral"),[historico,definirHistorico]=useState<string[]>([]),[revisao,definirRevisao]=useState(0);
  const [dados,definirDados]=useState<PaginaChat>(),[carregando,definirCarregando]=useState(true),[ocupada,definirOcupada]=useState(false);
  const [erroLeitura,definirErroLeitura]=useState(""),[erroAcao,definirErroAcao]=useState("");
  const [rascunhos,definirRascunhos]=useState<Record<string,string>>({}),[edicao,definirEdicao]=useState<{mensagem:MensagemChat;texto:string}>(),[remocao,definirRemocao]=useState<string>();
  const lista=useRef<HTMLDivElement>(null),seguirNovas=useRef(true);
  const antes=historico[historico.length-1],texto=rascunhos[canal]??"";
  useEffect(()=>{
    let ativa=true,consultando=false,bloqueada=false;
    async function carregar(){if(consultando||bloqueada)return;consultando=true;try{const pagina=await listarMensagens(canal,antes);if(ativa){definirDados(pagina);definirErroLeitura("");}}catch(e){if(ativa){definirErroLeitura(e instanceof Error?e.message:"Não foi possível carregar a conversa.");if(e instanceof ErroApi&&[401,403].includes(e.status)){definirDados(undefined);bloqueada=true;}}}finally{consultando=false;if(ativa)definirCarregando(false);}}
    void carregar();
    const consultarVisivel=()=>{if(!antes&&document.visibilityState==="visible")void carregar();};
    const timer=setInterval(consultarVisivel,3000);document.addEventListener("visibilitychange",consultarVisivel);
    return()=>{ativa=false;clearInterval(timer);document.removeEventListener("visibilitychange",consultarVisivel);};
  },[canal,antes,revisao]);
  useEffect(()=>{if(lista.current&&!antes&&seguirNovas.current)lista.current.scrollTop=lista.current.scrollHeight;},[dados,antes]);
  function atualizar(){definirRevisao(r=>r+1);}
  function mudarCanal(id:string){definirCanal(id);definirHistorico([]);definirDados(undefined);definirCarregando(true);definirEdicao(undefined);definirRemocao(undefined);definirErroAcao("");seguirNovas.current=true;}
  async function executar(acao:()=>Promise<unknown>,enviando=false){if(ocupada)return;definirOcupada(true);definirErroAcao("");try{await acao();if(enviando){definirRascunhos(r=>({...r,[canal]:""}));definirHistorico([]);seguirNovas.current=true;}definirEdicao(undefined);definirRemocao(undefined);atualizar();}catch(e){definirErroAcao(e instanceof Error?e.message:"Não foi possível concluir a ação.");if(e instanceof ErroApi&&[401,403].includes(e.status))definirDados(undefined);}finally{definirOcupada(false);}}
  function enviar(e:FormEvent<HTMLFormElement>){e.preventDefault();if(!dados?.podeEnviar||!texto.trim()||ocupada)return;void executar(()=>enviarMensagem(canal,texto),true);}
  return <div className="space-y-5"><div><h1 className="page-title">Chat</h1><p className="subtle">Converse com o Workspace e com suas equipes.</p></div>
    <label className="field">Canal<select value={canal} disabled={ocupada} onChange={e=>mudarCanal(e.target.value)}>{canais.map(c=><option key={c.id} value={c.id}>{c.nome}{c.arquivado?" · Histórico":""}</option>)}</select></label>
    {erroLeitura&&<ErrorState message={erroLeitura} onRetry={atualizar}/>}{erroAcao&&<ErrorState message={erroAcao} onRetry={atualizar}/>}
    <section className="workspace-panel space-y-4 p-4" aria-label="Conversa do canal">
      <div className="flex flex-wrap justify-between gap-2"><h2 className="min-w-0 max-w-full break-words font-semibold">{dados?.canal.nome??"Conversa"}</h2><Button variant="ghost" disabled={ocupada} onClick={atualizar}>Atualizar conversa</Button></div>
      {antes&&<p className="subtle text-sm">Você está consultando o histórico. A atualização automática fica pausada.</p>}
      {carregando?<p role="status">Carregando mensagens…</p>:dados&&<>
        <div className="flex flex-wrap gap-2">{dados.proximaPagina&&<Button variant="secondary" disabled={ocupada} onClick={()=>{definirHistorico(h=>[...h,dados.proximaPagina!]);definirCarregando(true);}}>Mensagens anteriores</Button>}{antes&&<Button variant="secondary" disabled={ocupada} onClick={()=>{definirHistorico(h=>h.slice(0,-1));definirCarregando(true);}}>Mais recentes</Button>}{antes&&<Button variant="ghost" disabled={ocupada} onClick={()=>{definirHistorico([]);definirCarregando(true);seguirNovas.current=true;}}>Voltar às recentes</Button>}</div>
        {!dados.items.length&&<EmptyState title="A conversa começa aqui." description={dados.podeEnviar?"Envie uma mensagem para este canal.":"Ainda não há mensagens neste canal."}/>}
        <div ref={lista} role="log" aria-label="Mensagens do canal" className="max-h-[55vh] space-y-3 overflow-y-auto" onScroll={()=>{if(lista.current)seguirNovas.current=lista.current.scrollHeight-lista.current.scrollTop-lista.current.clientHeight<80;}}>
          {dados.items.map(m=><article key={m.id} className={`max-w-full space-y-2 rounded-xl border border-border p-3 ${m.autora.id===pessoaId?"bg-primary/5":"bg-background"}`}>
            <div className="flex flex-wrap items-baseline justify-between gap-2"><h3 className="min-w-0 max-w-full break-words text-sm font-semibold">{m.autora.nome}</h3><p className="subtle text-xs">{new Date(m.criadaEm).toLocaleString("pt-BR")}{m.versao>0&&!m.removida?" · Editada":""}</p></div>
            {edicao?.mensagem.id===m.id&&!m.removida?<form className="space-y-2" onSubmit={e=>{e.preventDefault();if(edicao.texto.trim())void executar(()=>editarMensagem(edicao.mensagem,edicao.texto));}}>
              <label className="field">Editar mensagem<textarea value={edicao.texto} maxLength={2000} required disabled={ocupada} onChange={e=>definirEdicao({...edicao,texto:e.target.value})}/></label>
              <div className="flex flex-wrap gap-2"><Button type="submit" disabled={ocupada}>Salvar mensagem</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirEdicao(undefined)}>Cancelar edição</Button></div>
            </form>:<p className={`whitespace-pre-wrap break-words text-sm ${m.removida?"subtle italic":""}`}>{m.removida?"Mensagem removida.":m.texto}</p>}
            {remocao===m.id?<div className="space-y-2"><p className="text-sm">Remover esta mensagem? Ela deixa de aparecer na conversa.</p><div className="flex flex-wrap gap-2"><Button variant="danger" disabled={ocupada} onClick={()=>void executar(()=>removerMensagem(m))}>Confirmar remoção</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirRemocao(undefined)}>Cancelar remoção</Button></div></div>:<div className="flex flex-wrap gap-2">{m.capacidades.editar&&<Button variant="ghost" disabled={ocupada} aria-label={`Editar mensagem de ${m.autora.nome} em ${new Date(m.criadaEm).toLocaleString("pt-BR")}`} onClick={()=>definirEdicao({mensagem:m,texto:m.texto??""})}>Editar</Button>}{m.capacidades.remover&&<Button variant="ghost" disabled={ocupada} aria-label={`Remover mensagem de ${m.autora.nome} em ${new Date(m.criadaEm).toLocaleString("pt-BR")}`} onClick={()=>definirRemocao(m.id)}>Remover</Button>}</div>}
          </article>)}
        </div>
        {dados.podeEnviar?<form onSubmit={enviar} className="space-y-3"><label className="field">Mensagem<textarea rows={3} value={texto} maxLength={2000} required disabled={ocupada} onChange={e=>definirRascunhos(r=>({...r,[canal]:e.target.value}))}/></label><div className="flex flex-wrap items-center justify-between gap-2"><p className="subtle text-xs">Texto simples · até 2.000 caracteres</p><Button type="submit" disabled={ocupada||!texto.trim()}>{ocupada?"Aguarde…":"Enviar mensagem"}</Button></div></form>:<p className="subtle text-sm">Este canal está disponível somente para leitura.</p>}
      </>}
    </section>
  </div>;
}
