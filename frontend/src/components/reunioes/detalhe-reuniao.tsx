"use client";
import { useEffect,useState,type FormEvent } from "react";
import { criarReuniao,editarReuniao,arquivarReuniao,opcoesReunioes,statusReuniao,formatarHorarioReuniao,type DadosReuniao,type Reuniao,type OpcoesReunioes } from "@/lib/api/reunioes";
import { Dialog } from "../ui/dialog";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { ErrorState } from "../ui/feedback";
export function DetalheReuniao({inicial,opcoes,aoFechar,aoConcluir}:{inicial:Reuniao|null;opcoes:OpcoesReunioes;aoFechar:()=>void;aoConcluir:()=>void}) {
  const [modo,definirModo]=useState<"ver"|"editar"|"arquivar">(inicial?"ver":"editar"),[ocupada,definirOcupada]=useState(false),[erro,definirErro]=useState("");
  const [equipe,definirEquipe]=useState(inicial?.equipe.id??""),[pessoas,definirPessoas]=useState(opcoes.participantes);
  const [participantes,definirParticipantes]=useState(inicial?.participantes.map(p=>p.id)??[]),[responsaveis,definirResponsaveis]=useState(inicial?.participantes.filter(p=>p.responsavel).map(p=>p.id)??[]);
  const [carregando,definirCarregando]=useState(false);
  useEffect(()=>{if(!equipe)return;let ativa=true;opcoesReunioes(equipe).then(o=>{if(ativa){definirPessoas(o.participantes);definirCarregando(false);}}).catch(e=>{if(ativa){definirErro(e instanceof Error?e.message:"Não foi possível carregar participantes.");definirCarregando(false);}});return()=>{ativa=false;};},[equipe]);
  async function executar(acao:()=>Promise<unknown>) {definirOcupada(true);definirErro("");try{await acao();aoConcluir();}catch(e){definirErro(e instanceof Error?e.message:"Não foi possível concluir a operação.");}finally{definirOcupada(false);}}
  function salvar(e:FormEvent<HTMLFormElement>) {
    e.preventDefault();const f=new FormData(e.currentTarget);
    const dados:DadosReuniao={titulo:String(f.get("titulo")),pauta:String(f.get("pauta")),tipo:String(f.get("tipo")) as DadosReuniao["tipo"],status:String(f.get("status")) as DadosReuniao["status"],equipeId:equipe,inicioLocal:String(f.get("inicio")),fimLocal:String(f.get("fim")),zona:String(f.get("zona")),local:String(f.get("local")),link:String(f.get("link")),resultados:String(f.get("resultados")),participanteIds:participantes,responsavelIds:responsaveis};
    void executar(()=>inicial?editarReuniao(inicial,dados):criarReuniao(dados));
  }
  return <Dialog open onClose={aoFechar} busy={ocupada} title={!inicial?"Nova reunião / Talk":modo==="arquivar"?"Arquivar reunião?":"Detalhes da reunião"}>
    {erro&&<ErrorState message={erro} onRetry={aoConcluir}/>}
    {modo==="ver"&&inicial&&<div className="space-y-4"><h3 className="break-words text-xl font-semibold">{inicial.titulo}</h3><p className="subtle">{inicial.tipo==="TALK"?"Talk":"Reunião"} · {statusReuniao[inicial.status]}{inicial.arquivada?" · Arquivada":""}</p><p className="whitespace-pre-wrap break-words">{inicial.pauta||"Sem pauta."}</p><dl className="grid gap-3 text-sm sm:grid-cols-2"><div><dt className="subtle">Equipe</dt><dd>{inicial.equipe.nome}</dd></div><div><dt className="subtle">Fuso do encontro</dt><dd>{inicial.zona}</dd></div><div><dt className="subtle">Início</dt><dd>{formatarHorarioReuniao(inicial.inicio,inicial.zona)}</dd></div><div><dt className="subtle">Fim</dt><dd>{formatarHorarioReuniao(inicial.fim,inicial.zona)}</dd></div><div><dt className="subtle">Local</dt><dd className="break-words">{inicial.local||"Não informado"}</dd></div><div><dt className="subtle">Criada por</dt><dd>{inicial.criadaPor.nome}</dd></div></dl>
      {inicial.link&&<a className="break-all text-primary underline" href={inicial.link} target="_blank" rel="noopener noreferrer">Abrir link do encontro</a>}
      <div><h4 className="font-medium">Participantes</h4>{inicial.participantes.length?<ul className="text-sm">{inicial.participantes.map(p=><li key={p.id}>{p.nome}{p.responsavel?" · Responsável":""}</li>)}</ul>:<p className="subtle">Sem participantes definidos.</p>}</div>
      <div><h4 className="font-medium">Resultados</h4><p className="whitespace-pre-wrap break-words text-sm">{inicial.resultados||"Sem resultados registrados."}</p></div>
      <div className="flex flex-wrap gap-2">{inicial.capacidades.editar&&<Button onClick={()=>definirModo("editar")}>Editar reunião</Button>}{inicial.capacidades.arquivar&&<Button variant="secondary" onClick={()=>definirModo("arquivar")}>Arquivar reunião</Button>}</div>
    </div>}
    {modo==="editar"&&<form onSubmit={salvar} className="space-y-4"><label className="field">Título<Input name="titulo" defaultValue={inicial?.titulo} maxLength={200} required disabled={ocupada}/></label><label className="field">Pauta<textarea name="pauta" defaultValue={inicial?.pauta??""} maxLength={5000} rows={3} disabled={ocupada}/></label>
      <div className="grid gap-3 sm:grid-cols-2"><label className="field">Tipo<select name="tipo" defaultValue={inicial?.tipo??"REUNIAO"} disabled={ocupada}><option value="REUNIAO">Reunião</option><option value="TALK">Talk</option></select></label><label className="field">Status<select name="status" defaultValue={inicial?.status??"AGENDADA"} disabled={ocupada||!inicial}>{Object.entries(statusReuniao).map(([valor,nome])=><option key={valor} value={valor}>{nome}</option>)}</select>{!inicial&&<input type="hidden" name="status" value="AGENDADA"/>}</label></div>
      <label className="field">Equipe<select value={equipe} required disabled={ocupada} onChange={e=>{definirEquipe(e.target.value);definirParticipantes([]);definirResponsaveis([]);definirPessoas([]);definirCarregando(true);}}><option value="">Selecione</option>{opcoes.equipes.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
      <label className="field">Fuso do encontro<select name="zona" defaultValue={inicial?.zona??"America/Bahia"} disabled={ocupada}><option value="America/Bahia">America/Bahia</option><option value="Europe/Lisbon">Europe/Lisbon</option>{inicial&&!['America/Bahia','Europe/Lisbon'].includes(inicial.zona)&&<option>{inicial.zona}</option>}</select></label>
      <p className="text-xs subtle">Informe os horários no fuso selecionado. Datas de tarefas e projetos continuam sem horário.</p>
      <div className="grid gap-3 sm:grid-cols-2"><label className="field">Início<Input type="datetime-local" name="inicio" defaultValue={inicial?.inicioLocal.slice(0,16)} required disabled={ocupada}/></label><label className="field">Fim<Input type="datetime-local" name="fim" defaultValue={inicial?.fimLocal.slice(0,16)} required disabled={ocupada}/></label></div>
      <label className="field">Local<Input name="local" defaultValue={inicial?.local??""} maxLength={500} disabled={ocupada}/></label><label className="field">Link<Input name="link" type="url" defaultValue={inicial?.link??""} maxLength={2048} disabled={ocupada}/></label>
      <label className="field">Participantes<select multiple value={participantes} disabled={ocupada||!equipe||carregando} onChange={e=>definirParticipantes(Array.from(e.target.selectedOptions,o=>o.value))}>{pessoas.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
      <label className="field">Responsáveis<select multiple value={responsaveis} disabled={ocupada||!equipe||carregando} onChange={e=>definirResponsaveis(Array.from(e.target.selectedOptions,o=>o.value))}>{pessoas.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}</select></label><p className="text-xs subtle">Responsáveis também fazem parte dos participantes.</p>
      <label className="field">Resultados<textarea name="resultados" defaultValue={inicial?.resultados??""} maxLength={5000} rows={3} disabled={ocupada}/></label>
      <div className="flex gap-2"><Button type="submit" disabled={ocupada||carregando||!equipe}>{ocupada?"Salvando…":"Salvar reunião"}</Button><Button variant="secondary" disabled={ocupada} onClick={()=>inicial?definirModo("ver"):aoFechar()}>Cancelar</Button></div>
    </form>}
    {modo==="arquivar"&&inicial&&<div className="space-y-4"><p>Arquivar “{inicial.titulo}”? O registro permanece no histórico.</p><div className="flex gap-2"><Button variant="danger" disabled={ocupada} onClick={()=>void executar(()=>arquivarReuniao(inicial))}>Confirmar arquivamento</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirModo("ver")}>Cancelar</Button></div></div>}
  </Dialog>;
}
