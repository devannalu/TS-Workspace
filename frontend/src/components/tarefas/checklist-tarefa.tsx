"use client";
import { useCallback,useEffect,useId,useState } from "react";
import { ArrowDown,ArrowUp,Pencil,Trash2 } from "lucide-react";
import { listarChecklist,criarItemChecklist,editarItemChecklist,removerItemChecklist,ordenarChecklist,type ItemChecklist,type ListaChecklist } from "@/lib/api/checklist";
import { Input } from "../ui/input";
import { Button } from "../ui/button";
import { ErrorState } from "../ui/feedback";
export function ChecklistTarefa({tarefaId}:{tarefaId:string}) {
  const id=useId();
  const [dados,definirDados]=useState<ListaChecklist>(),[erro,definirErro]=useState(""),[ocupada,definirOcupada]=useState(false);
  const [texto,definirTexto]=useState(""),[editando,definirEditando]=useState<string>(),[edicao,definirEdicao]=useState(""),[removendo,definirRemovendo]=useState<string>();
  const carregar=useCallback(async()=>{try {definirDados(await listarChecklist(tarefaId));definirErro("");}catch(e){definirErro(e instanceof Error?e.message:"Não foi possível carregar o checklist.");}},[tarefaId]);
  useEffect(()=>{let ativa=true;listarChecklist(tarefaId).then(d=>{if(ativa)definirDados(d);}).catch(e=>{if(ativa)definirErro(e instanceof Error?e.message:"Não foi possível carregar o checklist.");});return()=>{ativa=false;};},[tarefaId]);
  async function executar(acao:()=>Promise<ListaChecklist>,finalizar?:()=>void) {
    definirOcupada(true);definirErro("");
    try {definirDados(await acao());finalizar?.();}catch(e){definirErro(e instanceof Error?e.message:"Não foi possível atualizar o checklist.");}
    finally{definirOcupada(false);}
  }
  function mover(item:ItemChecklist,direcao:number) {
    if(!dados)return;const itens=[...dados.items],indice=itens.findIndex(i=>i.id===item.id),destino=indice+direcao;
    if(destino<0||destino>=itens.length)return;[itens[indice],itens[destino]]=[itens[destino],itens[indice]];
    void executar(()=>ordenarChecklist(tarefaId,itens));
  }
  return <section aria-labelledby={`${id}-titulo`} className="space-y-3 rounded-xl border border-border p-3">
    <div className="flex items-center justify-between gap-2"><h4 id={`${id}-titulo`} className="font-semibold">Checklist</h4>{dados&&<p className="text-sm subtle" role="status">{dados.concluidos} de {dados.total}</p>}</div>
    {erro&&<ErrorState message={erro} onRetry={()=>void carregar()} />}
    {!dados&&!erro&&<p role="status">Carregando checklist…</p>}
    {dados&&<>{dados.total===0&&<p className="text-sm subtle">Nenhum item no checklist.</p>}
      <ol className="space-y-2">{dados.items.map((item,indice)=><li key={item.id} className="space-y-2 rounded-lg bg-muted p-2">
        <div className="flex items-start gap-2"><input type="checkbox" className="mt-1 size-5 shrink-0 accent-primary" aria-label={`Concluir ${item.texto}`} checked={item.concluido} disabled={ocupada||!dados.podeEditar} onChange={e=>void executar(()=>editarItemChecklist(tarefaId,item,item.texto,e.target.checked))} />
          <p className={`min-w-0 flex-1 break-words text-sm ${item.concluido?"line-through subtle":""}`}>{item.texto}</p></div>
        {dados.podeEditar&&<div className="flex flex-wrap gap-1">
          <Button variant="ghost" disabled={ocupada||indice===0} aria-label={`Mover ${item.texto} para cima`} onClick={()=>mover(item,-1)}><ArrowUp size={16} aria-hidden /></Button>
          <Button variant="ghost" disabled={ocupada||indice===dados.items.length-1} aria-label={`Mover ${item.texto} para baixo`} onClick={()=>mover(item,1)}><ArrowDown size={16} aria-hidden /></Button>
          <Button variant="ghost" disabled={ocupada} aria-label={`Editar ${item.texto}`} onClick={()=>{definirEditando(item.id);definirEdicao(item.texto);}}><Pencil size={16} aria-hidden /></Button>
          <Button variant="ghost" disabled={ocupada} aria-label={`Remover ${item.texto}`} onClick={()=>definirRemovendo(item.id)}><Trash2 size={16} aria-hidden /></Button>
        </div>}
        {editando===item.id&&<form className="space-y-2" onSubmit={e=>{e.preventDefault();void executar(()=>editarItemChecklist(tarefaId,item,edicao,item.concluido),()=>definirEditando(undefined));}}><label className="block text-sm">Texto do item<Input className="mt-1" value={edicao} onChange={e=>definirEdicao(e.target.value)} maxLength={500} required disabled={ocupada} /></label><div className="flex gap-2"><Button type="submit" disabled={ocupada||!edicao.trim()}>Salvar item</Button><Button type="button" variant="secondary" disabled={ocupada} onClick={()=>definirEditando(undefined)}>Cancelar</Button></div></form>}
        {removendo===item.id&&<div className="space-y-2"><p className="text-sm">Remover este item do checklist?</p><div className="flex gap-2"><Button variant="danger" disabled={ocupada} onClick={()=>void executar(()=>removerItemChecklist(tarefaId,item),()=>definirRemovendo(undefined))}>Confirmar remoção</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirRemovendo(undefined)}>Cancelar</Button></div></div>}
      </li>)}</ol>
      {dados.podeEditar&&<form className="space-y-2" onSubmit={e=>{e.preventDefault();void executar(()=>criarItemChecklist(tarefaId,texto),()=>definirTexto(""));}}><label className="block text-sm" htmlFor={`${id}-novo`}>Novo item</label><Input id={`${id}-novo`} value={texto} onChange={e=>definirTexto(e.target.value)} maxLength={500} required disabled={ocupada||dados.total>=200} /><Button type="submit" disabled={ocupada||!texto.trim()||dados.total>=200}>Adicionar item</Button></form>}
    </>}
  </section>;
}
