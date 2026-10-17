"use client";
import {useEffect,useState,type FormEvent} from "react";
import {listarAuditoria,buscarEventoAuditoria,nomeAcao,nomesRecursos,type EventoAuditoria,type PaginaAuditoria,type OpcoesAuditoria,type FiltrosAuditoria} from "@/lib/api/auditoria";
import {Input} from "../ui/input";
import {Button} from "../ui/button";
import {Pagination} from "../ui/pagination";
import {Dialog} from "../ui/dialog";
import {ErrorState,EmptyState} from "../ui/feedback";
const filtrosVazios:FiltrosAuditoria={autora:"",acao:"",tipo:"",desde:"",ate:""};
export function AuditoriaWorkspace({opcoes}:{opcoes:OpcoesAuditoria}) {
  const [filtros,definirFiltros]=useState(filtrosVazios),[consulta,definirConsulta]=useState(filtrosVazios),[pagina,definirPagina]=useState(0),[revisao,definirRevisao]=useState(0);
  const [dados,definirDados]=useState<PaginaAuditoria>(),[erro,definirErro]=useState(""),[carregando,definirCarregando]=useState(true),[selecionado,definirSelecionado]=useState<EventoAuditoria>(),[ocupada,definirOcupada]=useState(false);
  useEffect(()=>{let ativa=true;listarAuditoria(consulta,pagina).then(r=>{if(ativa)definirDados(r);}).catch(e=>{if(ativa){definirDados(undefined);definirSelecionado(undefined);definirErro(e instanceof Error?e.message:"Não foi possível carregar a auditoria.");}}).finally(()=>{if(ativa)definirCarregando(false);});return()=>{ativa=false;};},[consulta,pagina,revisao]);
  function campo(nome:keyof FiltrosAuditoria,valor:string){definirFiltros(f=>({...f,[nome]:valor}));}
  function aplicar(e:FormEvent){e.preventDefault();definirConsulta({...filtros});definirPagina(0);definirErro("");definirCarregando(true);definirRevisao(r=>r+1);}
  async function abrir(id:string){definirOcupada(true);definirErro("");try{definirSelecionado(await buscarEventoAuditoria(id));}catch(e){definirSelecionado(undefined);definirDados(undefined);definirErro(e instanceof Error?e.message:"Não foi possível abrir o registro.");}finally{definirOcupada(false);}}
  return <div className="space-y-6">
    <div><h1 className="page-title">Auditoria</h1><p className="subtle">Ações importantes do Workspace. Consulta administrativa, somente leitura.</p></div>
    <form onSubmit={aplicar} aria-label="Filtros de auditoria" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <label className="field">Nome da autora<Input value={filtros.autora} onChange={e=>campo("autora",e.target.value)} maxLength={200} placeholder="Nome de quem realizou a ação"/></label>
      <label className="field">Ação<select value={filtros.acao} onChange={e=>campo("acao",e.target.value)}><option value="">Todas as ações</option>{opcoes.acoes.map(a=><option key={a} value={a}>{nomeAcao(a)}</option>)}</select></label>
      <label className="field">Tipo de recurso<select value={filtros.tipo} onChange={e=>campo("tipo",e.target.value)}><option value="">Todos os recursos</option>{opcoes.tipos.map(t=><option key={t} value={t}>{nomesRecursos[t]??"Recurso"}</option>)}</select></label>
      <label className="field">Desde<Input type="date" value={filtros.desde} max={filtros.ate||undefined} onChange={e=>campo("desde",e.target.value)}/></label>
      <label className="field">Até<Input type="date" value={filtros.ate} min={filtros.desde||undefined} onChange={e=>campo("ate",e.target.value)}/></label>
      <div className="flex flex-wrap gap-2 items-end"><Button type="submit" disabled={carregando}>Aplicar filtros</Button><Button variant="secondary" onClick={()=>{definirFiltros(filtrosVazios);definirConsulta(filtrosVazios);definirPagina(0);definirCarregando(true);definirErro("");definirRevisao(r=>r+1);}}>Limpar</Button></div>
    </form>
    <p className="subtle text-xs">Sem período informado, exibimos os últimos 30 dias. Intervalo máximo de 366 dias; datas seguem o fuso deste dispositivo.</p>
    {erro&&<ErrorState message={erro} onRetry={()=>{definirErro("");definirCarregando(true);definirRevisao(r=>r+1);}}/>}
    {carregando?<p role="status">Carregando auditoria…</p>:dados&&<>
      {!dados.items.length&&<EmptyState title="Nenhum registro neste período." description="Ajuste os filtros para consultar outras ações."/>}
      {dados.items.map(evento=><article key={evento.id} className="rounded-xl border border-border bg-card p-4 space-y-2"><div className="flex flex-wrap items-center justify-between gap-2"><h2 className="font-medium">{nomeAcao(evento.acao)}</h2><time className="subtle text-xs" dateTime={evento.ocorridoEm}>{new Date(evento.ocorridoEm).toLocaleString("pt-BR")}</time></div><p className="text-sm break-words">{evento.autora?.nome??"Sistema / autora indisponível"}</p><Button variant="ghost" disabled={ocupada} onClick={()=>void abrir(evento.id)} aria-label={`Detalhes de ${nomeAcao(evento.acao)} em ${new Date(evento.ocorridoEm).toLocaleString("pt-BR")}`}>Ver detalhes</Button></article>)}
      <Pagination page={dados.page} size={dados.size} total={dados.total} label="registros" onPage={p=>{definirPagina(p);definirCarregando(true);definirErro("");}}/>
    </>}
    <Dialog open={Boolean(selecionado)} onClose={()=>definirSelecionado(undefined)} title="Detalhes do registro">{selecionado&&<dl className="space-y-4 text-sm"><div><dt className="subtle">Ação</dt><dd>{nomeAcao(selecionado.acao)}</dd></div><div><dt className="subtle">Autora</dt><dd className="break-words">{selecionado.autora?.nome??"Sistema / autora indisponível"}</dd></div><div><dt className="subtle">Quando</dt><dd>{new Date(selecionado.ocorridoEm).toLocaleString("pt-BR")}</dd></div><div><dt className="subtle">Tipo de recurso</dt><dd>{nomesRecursos[selecionado.tipo]??"Recurso"}</dd></div><div><dt className="subtle">Identificador do recurso</dt><dd className="break-all">{selecionado.recursoId??"Indisponível"}</dd></div><div><dt className="subtle">Identificador do registro</dt><dd className="break-all">{selecionado.id}</dd></div><p className="subtle">Este registro preserva a ação, sem expor conteúdo privado do recurso.</p></dl>}</Dialog>
  </div>;
}
