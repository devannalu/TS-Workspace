"use client";
import {useEffect,useState,type FormEvent} from "react";
import Link from "next/link";
import {buscarWorkspace,nomesBusca,type TipoBusca,type RespostaBusca} from "@/lib/api/busca";
import {Input} from "../ui/input";
import {Button} from "../ui/button";
import {ErrorState,EmptyState} from "../ui/feedback";
import {Pagination} from "../ui/pagination";

export function BuscaWorkspace({consultaInicial=""}:{consultaInicial?:string}) {
  const [consulta,definirConsulta]=useState(consultaInicial),[termo,definirTermo]=useState(consultaInicial);
  const [tipo,definirTipo]=useState<TipoBusca>(),[pagina,definirPagina]=useState(0),[revisao,definirRevisao]=useState(0);
  const [dados,definirDados]=useState<RespostaBusca>(),[erro,definirErro]=useState(""),[carregando,definirCarregando]=useState(consultaInicial.trim().length>=2);
  useEffect(()=>{
    if(termo.trim().length<2)return;
    let ativa=true;
    buscarWorkspace(termo,tipo,pagina).then(r=>{if(ativa)definirDados(r);}).catch(e=>{if(ativa){definirDados(undefined);definirErro(e instanceof Error?e.message:"Não foi possível buscar.");}}).finally(()=>{if(ativa)definirCarregando(false);});
    return()=>{ativa=false;};
  },[termo,tipo,pagina,revisao]);
  function pesquisar(e:FormEvent){e.preventDefault();definirErro("");definirDados(undefined);definirCarregando(true);definirPagina(0);definirTermo(consulta.trim());definirRevisao(r=>r+1);}
  function filtrar(valor:TipoBusca|undefined){definirTipo(valor);definirPagina(0);definirDados(undefined);definirErro("");definirCarregando(true);}
  return <div className="space-y-6">
    <div><h1 className="page-title">Busca global</h1><p className="subtle">Encontre informações nos recursos que você pode acessar.</p></div>
    <form onSubmit={pesquisar} className="flex flex-wrap items-end gap-3">
      <label className="field min-w-0 flex-1">Buscar no Workspace<Input value={consulta} onChange={e=>definirConsulta(e.target.value)} minLength={2} maxLength={160} required placeholder="Título, descrição ou informação…"/></label>
      <Button type="submit" disabled={carregando}>{carregando?"Buscando…":"Buscar"}</Button>
    </form>
    {erro&&<ErrorState message={erro} onRetry={()=>{definirErro("");definirCarregando(true);definirRevisao(r=>r+1);}}/>}
    {carregando&&<p role="status">Buscando no Workspace…</p>}
    {dados&&<>
      <label className="field max-w-xs">Tipo de recurso<select value={tipo??""} onChange={e=>filtrar(e.target.value?e.target.value as TipoBusca:undefined)}><option value="">Todos os tipos</option>{dados.tipos.map(t=><option key={t} value={t}>{nomesBusca[t]}</option>)}</select></label>
      {!dados.grupos.some(g=>g.total>0)&&<EmptyState title="Nenhum resultado encontrado." description="Tente outro termo. Recursos arquivados ou removidos não aparecem na busca."/>}
      {dados.grupos.filter(g=>g.total>0).map(grupo=><section key={grupo.tipo} aria-label={nomesBusca[grupo.tipo]} className="space-y-3">
        <div className="flex flex-wrap justify-between items-center gap-2"><h2 className="font-semibold">{nomesBusca[grupo.tipo]} <span className="subtle">({grupo.total})</span></h2>{!tipo&&grupo.total>grupo.items.length&&<Button variant="secondary" onClick={()=>filtrar(grupo.tipo)}>Ver todos: {nomesBusca[grupo.tipo]}</Button>}</div>
        {grupo.items.map(item=><Link key={item.id} href={item.rota} className="block rounded-xl border border-border bg-card p-4 hover:bg-muted"><h3 className="font-medium break-words">{item.titulo}</h3><p className="subtle text-xs break-words">{item.contexto}</p>{item.resumo&&<p className="mt-2 text-sm whitespace-pre-wrap break-words">{item.resumo}</p>}</Link>)}
        {tipo&&<Pagination page={grupo.page} size={grupo.size} total={grupo.total} label="resultados" onPage={p=>{definirPagina(p);definirDados(undefined);definirCarregando(true);}}/>}
      </section>)}
    </>}
    {termo.trim().length<2&&!carregando&&<EmptyState title="O que você quer encontrar?" description="Pesquise tarefas, projetos, reuniões, eventos, conteúdo, parcerias e notas."/>}
  </div>;
}
