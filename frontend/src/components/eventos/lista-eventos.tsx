"use client";
import { useEffect,useState } from "react";
import { listarEventos,buscarEvento,statusEvento,formatarHorarioEvento,type Evento,type OpcoesEventos,type PaginaEventos } from "@/lib/api/eventos";
import { Button } from "../ui/button";
import { ErrorState,EmptyState } from "../ui/feedback";
import { Pagination } from "../ui/pagination";
import { DetalheEvento } from "./detalhe-evento";
export function ListaEventos({opcoes,podeCriar,abrirInicial}:{opcoes:OpcoesEventos;podeCriar:boolean;abrirInicial?:string}) {
  const [busca,definirBusca]=useState(""),[equipe,definirEquipe]=useState(""),[status,definirStatus]=useState(""),[arquivadas,definirArquivadas]=useState(false),[pagina,definirPagina]=useState(0);
  const [dados,definirDados]=useState<PaginaEventos>(),[erro,definirErro]=useState(""),[carregando,definirCarregando]=useState(true),[revisao,definirRevisao]=useState(0),[detalhe,definirDetalhe]=useState<Evento|null>();
  useEffect(()=>{let ativa=true;const timer=setTimeout(()=>{listarEventos(busca,equipe,status,arquivadas,pagina).then(d=>{if(ativa)definirDados(d);}).catch(e=>{if(ativa)definirErro(e instanceof Error?e.message:"Não foi possível carregar eventos.");}).finally(()=>{if(ativa)definirCarregando(false);});},200);return()=>{ativa=false;clearTimeout(timer);};},[busca,equipe,status,arquivadas,pagina,revisao]);
  useEffect(()=>{if(!abrirInicial||!/^[0-9a-f-]{36}$/i.test(abrirInicial))return;let ativa=true;buscarEvento(abrirInicial).then(r=>{if(ativa)definirDetalhe(r);}).catch(e=>{if(ativa)definirErro(e instanceof Error?e.message:"Não foi possível abrir o evento.");});return()=>{ativa=false;};},[abrirInicial]);
  function atualizar(){definirErro("");definirCarregando(true);definirRevisao(r=>r+1);}
  function filtrar(){definirPagina(0);atualizar();}
  async function abrir(id:string){try{definirDetalhe(await buscarEvento(id));}catch(e){definirErro(e instanceof Error?e.message:"Não foi possível abrir o evento.");}}
  return <div className="space-y-6"><div className="flex flex-wrap items-start justify-between gap-3"><div><h1 className="page-title">Eventos</h1><p className="subtle">Organize eventos da comunidade, responsáveis e operação.</p></div>{podeCriar&&<Button onClick={()=>definirDetalhe(null)}>Novo evento</Button>}</div>
    <div className="grid gap-3 sm:grid-cols-3"><label className="field">Buscar<input type="search" value={busca} maxLength={160} onChange={e=>{definirBusca(e.target.value);filtrar();}}/></label><label className="field">Equipe<select value={equipe} onChange={e=>{definirEquipe(e.target.value);filtrar();}}><option value="">Todas as equipes</option>{opcoes.equipes.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}</select></label><label className="field">Status<select value={status} onChange={e=>{definirStatus(e.target.value);filtrar();}}><option value="">Todos</option>{Object.entries(statusEvento).map(([v,n])=><option key={v} value={v}>{n}</option>)}</select></label></div>
    <label className="flex min-h-11 items-center gap-2 text-sm"><input type="checkbox" checked={arquivadas} onChange={e=>{definirArquivadas(e.target.checked);filtrar();}}/>Mostrar arquivadas</label>
    {erro&&<ErrorState message={erro} onRetry={atualizar}/>}{carregando?<p role="status">Carregando eventos…</p>:dados&&<>{!dados.items.length&&<EmptyState title="Nenhum evento encontrado." description="Crie uma evento ou ajuste os filtros."/>}<div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{dados.items.map(r=><button type="button" key={r.id} onClick={()=>void abrir(r.id)} className="workspace-panel min-w-0 space-y-2 p-4 text-left" aria-label={`Abrir evento ${r.nome}`}><h2 className="break-words font-semibold">{r.nome}</h2><p className="text-sm subtle">{({PRESENCIAL:"Presencial",ONLINE:"Online",HIBRIDO:"Híbrido"})[r.formato]} · {statusEvento[r.status]}</p><p className="text-sm">{formatarHorarioEvento(r.inicio,r.zona)}</p><p className="text-xs subtle">{r.zona} · {r.equipe.nome}</p></button>)}</div><Pagination page={dados.page} size={dados.size} total={dados.total} label="eventos" onPage={p=>{definirPagina(p);atualizar();}}/></>}
    {detalhe!==undefined&&<DetalheEvento inicial={detalhe} opcoes={opcoes} aoFechar={()=>definirDetalhe(undefined)} aoConcluir={()=>{definirDetalhe(undefined);atualizar();}}/>}
  </div>;
}
