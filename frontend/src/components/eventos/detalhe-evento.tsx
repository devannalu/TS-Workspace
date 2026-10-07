"use client";
import { useEffect,useState,type FormEvent } from "react";
import { criarEvento,editarEvento,arquivarEvento,opcoesEventos,statusEvento,formatarHorarioEvento,type DadosEvento,type Evento,type OpcoesEventos } from "@/lib/api/eventos";
import { Dialog } from "../ui/dialog";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { ErrorState } from "../ui/feedback";
export function DetalheEvento({inicial,opcoes,aoFechar,aoConcluir}:{inicial:Evento|null;opcoes:OpcoesEventos;aoFechar:()=>void;aoConcluir:()=>void}) {
  const [modo,definirModo]=useState<"ver"|"editar"|"arquivar">(inicial?"ver":"editar"),[ocupada,definirOcupada]=useState(false),[erro,definirErro]=useState("");
  const [equipe,definirEquipe]=useState(inicial?.equipe.id??""),[pessoas,definirPessoas]=useState(opcoes.responsaveis);
  const [responsaveis,definirResponsaveis]=useState(inicial?.responsaveis.map(p=>p.id)??[]);
  const [carregando,definirCarregando]=useState(false);
  useEffect(()=>{if(!equipe)return;let ativa=true;opcoesEventos(equipe).then(o=>{if(ativa){definirPessoas(o.responsaveis);definirCarregando(false);}}).catch(e=>{if(ativa){definirErro(e instanceof Error?e.message:"Não foi possível carregar responsáveis.");definirCarregando(false);}});return()=>{ativa=false;};},[equipe]);
  async function executar(acao:()=>Promise<unknown>) {definirOcupada(true);definirErro("");try{await acao();aoConcluir();}catch(e){definirErro(e instanceof Error?e.message:"Não foi possível concluir a operação.");}finally{definirOcupada(false);}}
  function salvar(e:FormEvent<HTMLFormElement>) {
    e.preventDefault();const f=new FormData(e.currentTarget);
    const dados:DadosEvento={nome:String(f.get("nome")),descricao:String(f.get("descricao")),formato:String(f.get("formato")) as DadosEvento["formato"],status:String(f.get("status")) as DadosEvento["status"],equipeId:equipe,inicioLocal:String(f.get("inicio")),fimLocal:String(f.get("fim")),zona:String(f.get("zona")),local:String(f.get("local")),link:String(f.get("link")),notas:String(f.get("notas")),responsavelIds:responsaveis};
    void executar(()=>inicial?editarEvento(inicial,dados):criarEvento(dados));
  }
  return <Dialog open onClose={aoFechar} busy={ocupada} title={!inicial?"Novo evento":modo==="arquivar"?"Arquivar evento?":"Detalhes do evento"}>
    {erro&&<ErrorState message={erro} onRetry={aoConcluir}/>}
    {modo==="ver"&&inicial&&<div className="space-y-4"><h3 className="break-words text-xl font-semibold">{inicial.nome}</h3><p className="subtle">{({PRESENCIAL:"Presencial",ONLINE:"Online",HIBRIDO:"Híbrido"})[inicial.formato]} · {statusEvento[inicial.status]}{inicial.arquivada?" · Arquivado":""}</p><p className="whitespace-pre-wrap break-words">{inicial.descricao||"Sem descrição."}</p><dl className="grid gap-3 text-sm sm:grid-cols-2"><div><dt className="subtle">Equipe</dt><dd>{inicial.equipe.nome}</dd></div><div><dt className="subtle">Fuso do evento</dt><dd>{inicial.zona}</dd></div><div><dt className="subtle">Início</dt><dd>{formatarHorarioEvento(inicial.inicio,inicial.zona)}</dd></div><div><dt className="subtle">Fim</dt><dd>{formatarHorarioEvento(inicial.fim,inicial.zona)}</dd></div><div><dt className="subtle">Local</dt><dd className="break-words">{inicial.local||"Não informado"}</dd></div><div><dt className="subtle">Criado por</dt><dd>{inicial.criadaPor.nome}</dd></div></dl>
      {inicial.link&&<a className="break-all text-primary underline" href={inicial.link} target="_blank" rel="noopener noreferrer">Abrir link do evento</a>}
      <div><h4 className="font-medium">Responsáveis</h4>{inicial.responsaveis.length?<ul className="text-sm">{inicial.responsaveis.map(p=><li key={p.id}>{p.nome}</li>)}</ul>:<p className="subtle">Sem responsáveis definidos.</p>}</div>
      <div><h4 className="font-medium">Notas operacionais</h4><p className="whitespace-pre-wrap break-words text-sm">{inicial.notas||"Sem notas registradas."}</p></div>
      <div className="flex flex-wrap gap-2">{inicial.capacidades.editar&&<Button onClick={()=>definirModo("editar")}>Editar evento</Button>}{inicial.capacidades.arquivar&&<Button variant="secondary" onClick={()=>definirModo("arquivar")}>Arquivar evento</Button>}</div>
    </div>}
    {modo==="editar"&&<form onSubmit={salvar} className="space-y-4"><label className="field">Nome<Input name="nome" defaultValue={inicial?.nome} maxLength={200} required disabled={ocupada}/></label><label className="field">Descrição<textarea name="descricao" defaultValue={inicial?.descricao??""} maxLength={5000} rows={3} disabled={ocupada}/></label>
      <div className="grid gap-3 sm:grid-cols-2"><label className="field">Formato<select name="formato" defaultValue={inicial?.formato??"PRESENCIAL"} disabled={ocupada}><option value="PRESENCIAL">Presencial</option><option value="ONLINE">Online</option><option value="HIBRIDO">Híbrido</option></select></label><label className="field">Status<select name="status" defaultValue={inicial?.status??"PLANEJADO"} disabled={ocupada||!inicial}>{Object.entries(statusEvento).map(([valor,nome])=><option key={valor} value={valor}>{nome}</option>)}</select>{!inicial&&<input type="hidden" name="status" value="PLANEJADO"/>}</label></div>
      <label className="field">Equipe<select value={equipe} required disabled={ocupada} onChange={e=>{definirEquipe(e.target.value);definirResponsaveis([]);definirPessoas([]);definirCarregando(true);}}><option value="">Selecione</option>{opcoes.equipes.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
      <label className="field">Fuso do evento<select name="zona" defaultValue={inicial?.zona??"America/Bahia"} disabled={ocupada}><option value="America/Bahia">America/Bahia</option><option value="Europe/Lisbon">Europe/Lisbon</option>{inicial&&!['America/Bahia','Europe/Lisbon'].includes(inicial.zona)&&<option>{inicial.zona}</option>}</select></label>
      <p className="text-xs subtle">Informe os horários no fuso selecionado. Datas de tarefas e projetos continuam sem horário.</p>
      <div className="grid gap-3 sm:grid-cols-2"><label className="field">Início<Input type="datetime-local" name="inicio" defaultValue={inicial?.inicioLocal.slice(0,16)} required disabled={ocupada}/></label><label className="field">Fim<Input type="datetime-local" name="fim" defaultValue={inicial?.fimLocal.slice(0,16)} required disabled={ocupada}/></label></div>
      <label className="field">Local<Input name="local" defaultValue={inicial?.local??""} maxLength={500} disabled={ocupada}/></label><label className="field">Link<Input name="link" type="url" defaultValue={inicial?.link??""} maxLength={2048} disabled={ocupada}/></label>

      <label className="field">Responsáveis<select multiple value={responsaveis} disabled={ocupada||!equipe||carregando} onChange={e=>definirResponsaveis(Array.from(e.target.selectedOptions,o=>o.value))}>{pessoas.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
      <label className="field">Notas operacionais<textarea name="notas" defaultValue={inicial?.notas??""} maxLength={5000} rows={3} disabled={ocupada}/></label>
      <div className="flex gap-2"><Button type="submit" disabled={ocupada||carregando||!equipe}>{ocupada?"Salvando…":"Salvar evento"}</Button><Button variant="secondary" disabled={ocupada} onClick={()=>inicial?definirModo("ver"):aoFechar()}>Cancelar</Button></div>
    </form>}
    {modo==="arquivar"&&inicial&&<div className="space-y-4"><p>Arquivar “{inicial.nome}”? O registro permanece no histórico.</p><div className="flex gap-2"><Button variant="danger" disabled={ocupada} onClick={()=>void executar(()=>arquivarEvento(inicial))}>Confirmar arquivamento</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirModo("ver")}>Cancelar</Button></div></div>}
  </Dialog>;
}
