"use client";
import {useEffect,useState,type FormEvent} from "react";
import {listarNotas,criarNota,editarNota,removerNota,type ContextoNota,type Nota,type PaginaNotas} from "@/lib/api/notas";
import {Button} from "../ui/button";
import {Input} from "../ui/input";
import {ErrorState,EmptyState} from "../ui/feedback";
import {Pagination} from "../ui/pagination";

export function NotasContextuais({recurso,recursoId}:{recurso:ContextoNota;recursoId:string}) {
  const [dados,definirDados]=useState<PaginaNotas>(),[pagina,definirPagina]=useState(0),[revisao,definirRevisao]=useState(0);
  const [erro,definirErro]=useState(""),[carregando,definirCarregando]=useState(true),[ocupada,definirOcupada]=useState(false);
  const [edicao,definirEdicao]=useState<Nota|null>(),[remocao,definirRemocao]=useState<string>();
  useEffect(()=>{let ativa=true;listarNotas(recurso,recursoId,pagina).then(r=>{if(ativa){definirDados(r);definirErro("");}}).catch(e=>{if(ativa)definirErro(e instanceof Error?e.message:"Não foi possível carregar notas.");}).finally(()=>{if(ativa)definirCarregando(false);});return()=>{ativa=false;};},[recurso,recursoId,pagina,revisao]);
  function atualizar(){definirCarregando(true);definirRevisao(r=>r+1);}
  async function executar(acao:()=>Promise<unknown>){definirOcupada(true);definirErro("");try{await acao();definirEdicao(undefined);definirRemocao(undefined);atualizar();}catch(e){definirErro(e instanceof Error?e.message:"Não foi possível salvar a nota.");}finally{definirOcupada(false);}}
  function salvar(e:FormEvent<HTMLFormElement>){e.preventDefault();const formulario=new FormData(e.currentTarget),titulo=String(formulario.get("tituloNota")),texto=String(formulario.get("textoNota"));void executar(()=>edicao?editarNota(edicao,titulo,texto):criarNota(recurso,recursoId,titulo,texto));}
  return <section aria-label="Notas contextuais" className="space-y-4 border-t border-border pt-5">
    <div className="flex flex-wrap items-center justify-between gap-2"><h4 className="font-semibold">Notas</h4>{dados?.podeCriar&&edicao===undefined&&<Button variant="secondary" onClick={()=>definirEdicao(null)}>Nova nota</Button>}</div>
    <p className="subtle text-sm">Informações e documentação deste recurso. Conversas ficam nos comentários, quando disponíveis.</p>
    {erro&&<ErrorState message={erro} onRetry={atualizar}/>}
    {edicao!==undefined&&<form onSubmit={salvar} className="space-y-3" aria-label={edicao?"Editar nota":"Nova nota"}>
      <label className="field">Título da nota<Input key={edicao?.id??"nova"} name="tituloNota" defaultValue={edicao?.titulo??""} maxLength={200} required disabled={ocupada}/></label>
      <label className="field">Texto da nota<textarea key={edicao?.id??"nova"} name="textoNota" defaultValue={edicao?.texto??""} rows={4} maxLength={5000} required disabled={ocupada}/></label>
      <div className="flex flex-wrap gap-2"><Button type="submit" disabled={ocupada}>{ocupada?"Salvando…":"Salvar nota"}</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirEdicao(undefined)}>Cancelar nota</Button></div>
    </form>}
    {carregando?<p role="status">Carregando notas…</p>:dados&&<>{!dados.items.length&&<EmptyState title="Nenhuma nota ainda." description="Registre informações úteis para este recurso."/>}
      {dados.items.map(n=><article key={n.id} className="rounded-xl border border-border p-3 space-y-2">
        <h5 className="font-semibold break-words">{n.titulo}</h5><p className="whitespace-pre-wrap break-words text-sm">{n.texto}</p>
        <p className="subtle text-xs">{n.autora.nome} · Atualizada em {new Date(n.atualizadaEm).toLocaleString("pt-BR")}</p>
        {remocao===n.id?<div className="space-y-2"><p className="text-sm">Remover esta nota? Ela deixa de aparecer no recurso.</p><div className="flex flex-wrap gap-2"><Button variant="danger" disabled={ocupada} onClick={()=>void executar(()=>removerNota(n))}>Confirmar remoção da nota</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirRemocao(undefined)}>Cancelar remoção</Button></div></div>:<div className="flex flex-wrap gap-2">{n.editar&&<Button variant="ghost" aria-label={`Editar nota ${n.titulo}`} disabled={ocupada} onClick={()=>definirEdicao(n)}>Editar</Button>}{n.remover&&<Button variant="ghost" aria-label={`Remover nota ${n.titulo}`} disabled={ocupada} onClick={()=>definirRemocao(n.id)}>Remover</Button>}</div>}
      </article>)}<Pagination page={dados.page} size={dados.size} total={dados.total} label="notas" onPage={p=>{definirPagina(p);definirCarregando(true);}}/></>}
  </section>;
}
