"use client";
import { useEffect, useState } from "react";
import { CalendarDays, ChevronLeft, ChevronRight, CheckCircle2 } from "lucide-react";
import { Button } from "../ui/button";
import { ErrorState, EmptyState } from "../ui/feedback";
import { buscarCalendario, type ItemCalendario } from "@/lib/api/calendario";
import { buscarTarefa, buscarOpcoesTarefas, statusTarefa, prioridadesTarefa, type Tarefa, type OpcoesTarefas, type ReferenciaTarefa } from "@/lib/api/tarefas";
import { buscarProjeto, buscarOpcoesProjetos, statusProjeto, type Projeto, type OpcoesProjetos } from "@/lib/api/projetos";
import { DetalheTarefa } from "../tarefas/detalhe-tarefa";
import { DetalheProjeto } from "../projetos/detalhe-projeto";
import { hojeLocal, dataValida, intervaloVisivel, navegarPeriodo, diasIntervalo, formatarData, type VisaoCalendario } from "./datas";

const visoes = { month: "Mês", week: "Semana", agenda: "Agenda" } as const;
export function Calendario({ usuarioId, permissoes, equipes, dataInicial, visaoInicial }: {
  usuarioId: string; permissoes: string[]; equipes: ReferenciaTarefa[]; dataInicial?: string; visaoInicial?: string;
}) {
  const [data, definirData] = useState(() => dataInicial && dataValida(dataInicial) ? dataInicial : hojeLocal());
  const [visao, definirVisao] = useState<VisaoCalendario>(() => visaoInicial && Object.hasOwn(visoes, visaoInicial) ? visaoInicial as VisaoCalendario : "month");
  const [diaSelecionado, definirDiaSelecionado] = useState(data);
  const [equipeId, definirEquipe] = useState("");
  const [tipo, definirTipo] = useState("");
  const [responsavelId, definirResponsavel] = useState("");
  const [meusItens, definirMeusItens] = useState(false);
  const [responsaveis, definirResponsaveis] = useState<ReferenciaTarefa[]>([]);
  const [itens, definirItens] = useState<ItemCalendario[]>([]);
  const [carregando, definirCarregando] = useState(true);
  const [erro, definirErro] = useState("");
  const [revisao, definirRevisao] = useState(0);
  const [abrindo, definirAbrindo] = useState(false);
  const [tarefa, definirTarefa] = useState<{ recurso: Tarefa; opcoes: OpcoesTarefas }>();
  const [projeto, definirProjeto] = useState<{ recurso: Projeto; opcoes: OpcoesProjetos }>();
  const podeTarefas = permissoes.includes("tasks.view"), podeProjetos = permissoes.includes("projects.view");
  const { de, ate } = intervaloVisivel(data, visao);
  const hoje = hojeLocal();

  useEffect(() => {
    let ativo = true;
    buscarCalendario(de, ate, equipeId, tipo, meusItens ? usuarioId : responsavelId)
      .then(resposta => { if (ativo) definirItens(resposta); })
      .catch(e => { if (ativo) definirErro(e instanceof Error ? e.message : "Não foi possível carregar o calendário."); })
      .finally(() => { if (ativo) definirCarregando(false); });
    return () => { ativo = false; };
  }, [de, ate, equipeId, tipo, responsavelId, meusItens, usuarioId, revisao]);

  useEffect(() => {
    if (!equipeId) return;
    let ativo = true;
    const consulta = podeTarefas ? buscarOpcoesTarefas(equipeId) : buscarOpcoesProjetos(equipeId);
    consulta.then(opcoes => { if (ativo) definirResponsaveis(opcoes.responsaveis); })
      .catch(e => { if (ativo) definirErro(e instanceof Error ? e.message : "Não foi possível carregar responsáveis."); });
    return () => { ativo = false; };
  }, [equipeId, podeTarefas]);

  function prepararConsulta() { definirCarregando(true); definirErro(""); }
  function atualizarPeriodo(novaData: string, novaVisao = visao) {
    if (!dataValida(novaData)) return;
    prepararConsulta(); definirData(novaData); definirDiaSelecionado(novaData); definirVisao(novaVisao); definirRevisao(r => r + 1);
    const parametros = new URLSearchParams({ view: novaVisao, date: novaData });
    window.history.replaceState(null, "", `/calendario?${parametros}`);
  }
  async function abrir(item: ItemCalendario) {
    definirAbrindo(true); definirErro("");
    try {
      if (item.tipo === "TAREFA") {
        const recurso = await buscarTarefa(item.recursoId);
        const opcoes = await buscarOpcoesTarefas(recurso.capacidades.editar ? recurso.equipe.id : undefined);
        definirTarefa({ recurso, opcoes });
      } else {
        const recurso = await buscarProjeto(item.recursoId);
        const opcoes = await buscarOpcoesProjetos(recurso.capacidades.editar ? recurso.equipe.id : undefined);
        definirProjeto({ recurso, opcoes });
      }
    } catch(e) { definirErro(e instanceof Error ? e.message : "Não foi possível abrir este recurso."); }
    finally { definirAbrindo(false); }
  }
  function concluir() {
    definirTarefa(undefined); definirProjeto(undefined); prepararConsulta(); definirRevisao(r => r + 1);
  }
  function itensDoDia(dia: string) { return itens.filter(item => item.dataInicio <= dia && item.dataFim >= dia); }
  function botaoItem(item: ItemCalendario, compacto = false) {
    const status = item.tipo === "TAREFA" ? statusTarefa[item.status as keyof typeof statusTarefa] : statusProjeto[item.status as keyof typeof statusProjeto];
    return <button key={item.id} type="button" disabled={abrindo} onClick={() => void abrir(item)}
      aria-label={`Abrir ${item.tipo === "TAREFA" ? "tarefa" : "projeto"} ${item.titulo}`}
      className={`block w-full min-w-0 rounded-lg border border-border px-3 py-2 text-left text-xs ${item.tipo === "TAREFA" ? "bg-accent" : "bg-lilac"} ${item.concluido ? "opacity-65" : ""}`}>
      <span className="flex items-center gap-1 font-medium">{item.concluido && <CheckCircle2 size={14} aria-hidden />}
        <span className="truncate">{item.titulo}</span></span>
      <span className="block">{item.tipo === "TAREFA" ? "Tarefa" : "Projeto"} · {status}</span>
      {!compacto && <><span className="block">{item.equipe.nome} · {formatarData(item.dataInicio, {day:"numeric",month:"short"})}
        {item.dataFim !== item.dataInicio && ` → ${formatarData(item.dataFim, {day:"numeric",month:"short"})}`}</span>
        {item.responsaveis.length > 0 && <span className="block">{item.responsaveis.map(p => p.nome).join(", ")}</span>}</>}
      {item.prioridade && <span className="inline-block">Prioridade {prioridadesTarefa[item.prioridade]}</span>}
      {item.atrasado && <span className="ml-2 inline-block font-semibold text-danger">Atrasado</span>}
    </button>;
  }
  const tituloPeriodo = visao === "week" ? `${formatarData(de, {day:"numeric",month:"short"})} – ${formatarData(ate)}`
    : formatarData(data, {month:"long",year:"numeric"});
  const dias = diasIntervalo(de, ate);
  const diasAgenda = dias.filter(dia => itens.some(item => (item.dataInicio < de ? de : item.dataInicio) === dia));

  return <div className="space-y-6">
    <header><h1 className="page-title">Calendário</h1><p className="subtle mt-2">Prazos e períodos das suas equipes em um só lugar.</p></header>
    <div className="workspace-panel space-y-4 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-1" role="group" aria-label="Visão do calendário">
          {Object.entries(visoes).map(([chave, nome]) => <Button key={chave} variant={visao === chave ? "primary" : "ghost"}
            aria-pressed={visao === chave} onClick={() => atualizarPeriodo(data, chave as VisaoCalendario)}>{nome}</Button>)}
        </div>
        <div className="flex items-center gap-1"><Button variant="secondary" onClick={() => atualizarPeriodo(hoje)}>Hoje</Button>
          <Button variant="ghost" aria-label="Período anterior" onClick={() => atualizarPeriodo(navegarPeriodo(data, visao, -1))}><ChevronLeft size={18} /></Button>
          <Button variant="ghost" aria-label="Próximo período" onClick={() => atualizarPeriodo(navegarPeriodo(data, visao, 1))}><ChevronRight size={18} /></Button></div>
      </div>
      <h2 aria-live="polite" className="section-title capitalize">{tituloPeriodo}</h2>
      <div className="grid gap-3 sm:grid-cols-3">
        <label className="field">Equipe<select value={equipeId} onChange={e => { prepararConsulta(); definirEquipe(e.target.value); definirResponsavel(""); definirResponsaveis([]); }}>
          <option value="">Todas as equipes</option>{equipes.map(e => <option key={e.id} value={e.id}>{e.nome}</option>)}</select></label>
        <label className="field">Tipo<select value={tipo} onChange={e => { prepararConsulta(); definirTipo(e.target.value); }}>
          <option value="">Todos os tipos disponíveis</option>{podeTarefas && <option value="TAREFA">Tarefas</option>}{podeProjetos && <option value="PROJETO">Projetos</option>}</select></label>
        <label className="field">Responsável<select disabled={!equipeId || meusItens} value={responsavelId} onChange={e => { prepararConsulta(); definirResponsavel(e.target.value); }}>
          <option value="">{equipeId ? "Todas as responsáveis" : "Selecione uma equipe"}</option>{responsaveis.map(p => <option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
      </div>
      <label className="flex min-h-11 items-center gap-2 text-sm"><input type="checkbox" checked={meusItens}
        onChange={e => { prepararConsulta(); definirMeusItens(e.target.checked); }} />Meus itens</label>
    </div>
    {erro && <ErrorState message={erro} onRetry={() => { prepararConsulta(); definirRevisao(r => r + 1); }} />}
    {abrindo && <p role="status" className="subtle">Abrindo detalhes…</p>}
    <section aria-label={`Calendário em ${visoes[visao].toLowerCase()}`} aria-busy={carregando} className="workspace-panel p-3 sm:p-4">
      {carregando ? <div role="status" aria-label="Carregando calendário" className="grid grid-cols-7 gap-1">
        {Array.from({length:28},(_,i) => <div key={i} className="h-14 animate-pulse rounded bg-muted sm:h-20" />)}</div>
      : <>
        {!itens.length && !erro && <EmptyState title="Nenhum prazo ou projeto neste período." description="Tarefas sem prazo e projetos sem datas continuam nas suas áreas." />}
        {visao === "agenda" ? <div className="space-y-5">{diasAgenda.map(dia => <section key={dia} aria-label={formatarData(dia)}>
          <h3 className="mb-2 font-medium">{dia === hoje ? "Hoje · " : ""}{formatarData(dia)}</h3>
          <div className="grid gap-2 md:grid-cols-2">{itens.filter(item => (item.dataInicio < de ? de : item.dataInicio) === dia).map(item => botaoItem(item))}</div>
        </section>)}</div> : <>
          <div className={visao === "week" ? "overflow-x-auto" : ""}><div className={`grid gap-px overflow-hidden rounded-xl border border-border bg-border ${visao === "month" ? "grid-cols-7" : "min-w-[700px] grid-cols-7"}`}>
            {visao === "month" && ["Seg","Ter","Qua","Qui","Sex","Sáb","Dom"].map(d => <div key={d} className="bg-muted py-2 text-center text-xs font-medium">{d}</div>)}
            {dias.map((dia, indice) => <div key={dia} className={`min-w-0 p-1 sm:p-2 ${indice % 7 > 4 ? "bg-background" : "bg-card"} ${dia.slice(0,7) !== data.slice(0,7) && visao === "month" ? "opacity-65" : ""}`}>
              <button type="button" aria-label={formatarData(dia)} aria-current={dia === hoje ? "date" : undefined}
                aria-pressed={dia === diaSelecionado} onClick={() => definirDiaSelecionado(dia)}
                className={`min-h-11 w-full rounded-lg text-xs ${dia === diaSelecionado ? "border border-primary bg-accent font-bold" : ""}`}>
                {visao === "week" ? formatarData(dia, {weekday:"short",day:"numeric"}) : Number(dia.slice(8))}
                {dia === hoje && <span className="block text-[10px] font-semibold">Hoje</span>}
              </button>
              <div className={visao === "month" ? "hidden space-y-1 md:block" : "space-y-1"}>
                {itensDoDia(dia).slice(0,3).map(item => botaoItem(item,true))}
                {itensDoDia(dia).length > 3 && <button className="min-h-11 w-full text-xs font-medium" onClick={() => definirDiaSelecionado(dia)}>+{itensDoDia(dia).length - 3} itens</button>}
              </div>
              {visao === "month" && itensDoDia(dia).length > 0 && <p aria-label={`${itensDoDia(dia).length} itens`} className="text-center text-xs md:hidden">{itensDoDia(dia).length} <CalendarDays size={12} aria-hidden className="inline" /></p>}
            </div>)}
          </div>
          </div><section className="mt-4" aria-label="Itens do dia selecionado"><h3 className="mb-2 font-medium">{formatarData(diaSelecionado)}</h3>
            {itensDoDia(diaSelecionado).length ? <div className="grid gap-2 md:grid-cols-2">{itensDoDia(diaSelecionado).map(item => botaoItem(item))}</div> : <p className="subtle">Nenhum item neste dia.</p>}</section>
        </>}
      </>}
    </section>
    {tarefa && <DetalheTarefa inicial={tarefa.recurso} opcoes={tarefa.opcoes} podeAtribuir={permissoes.includes("tasks.assign")}
      podeVerProjetos={podeProjetos} aoFechar={() => definirTarefa(undefined)} aoConcluir={concluir} />}
    {projeto && <DetalheProjeto inicial={projeto.recurso} opcoes={projeto.opcoes} podeGerenciar={permissoes.includes("projects.manage_members")}
      podeVerTarefas={podeTarefas} aoFechar={() => definirProjeto(undefined)} aoConcluir={concluir} />}
  </div>;
}
