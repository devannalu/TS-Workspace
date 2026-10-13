"use client";
import { useEffect,useState,type FormEvent } from "react";
import Link from "next/link";
import { criarParceria,editarParceria,arquivarParceria,opcoesParcerias,statusParceria,type DadosParceria,type Parceria,type OpcoesParcerias } from "@/lib/api/parcerias";
import { Dialog } from "../ui/dialog";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { ErrorState } from "../ui/feedback";
import { formatarData } from "../calendario/datas";
export function DetalheParceria({inicial,opcoes,aoFechar,aoConcluir}:{inicial:Parceria|null;opcoes:OpcoesParcerias;aoFechar:()=>void;aoConcluir:()=>void}) {
  const [modo,definirModo]=useState<"ver"|"editar"|"arquivar">(inicial?"ver":"editar");
  const [ocupada,definirOcupada]=useState(false),[erro,definirErro]=useState("");
  const [equipe,definirEquipe]=useState(inicial?.equipe.id??""),[disponiveis,definirDisponiveis]=useState(opcoes);
  const [responsavel,definirResponsavel]=useState(inicial?.responsavel?.id??"");
  const [evento,definirEvento]=useState(inicial?.eventoId??""),[projeto,definirProjeto]=useState(inicial?.projetoId??"");
  const [carregando,definirCarregando]=useState(false);
  useEffect(()=>{if(!equipe)return;let ativa=true;opcoesParcerias(equipe).then(o=>{if(ativa){definirDisponiveis(o);definirCarregando(false);}}).catch(e=>{if(ativa){definirErro(e instanceof Error?e.message:"Não foi possível carregar opções.");definirCarregando(false);}});return()=>{ativa=false;};},[equipe]);
  async function executar(acao:()=>Promise<unknown>) {definirOcupada(true);definirErro("");try{await acao();aoConcluir();}catch(e){definirErro(e instanceof Error?e.message:"Não foi possível concluir a operação.");}finally{definirOcupada(false);}}
  function salvar(e:FormEvent<HTMLFormElement>) {
    e.preventDefault();const f=new FormData(e.currentTarget);
    const dados:DadosParceria={organizacao:String(f.get("organizacao")),descricao:String(f.get("descricao")),contatoPrincipal:String(f.get("contatoPrincipal")),email:String(f.get("email"))||null,telefone:String(f.get("telefone")),proximaAcao:String(f.get("proximaAcao")),observacoes:String(f.get("observacoes")),status:String(f.get("status")) as Parceria["status"],equipeId:equipe,responsavelId:responsavel||null,dataProximaAcao:String(f.get("publicacao"))||null,eventoId:evento||null,projetoId:projeto||null};
    void executar(()=>inicial?editarParceria(inicial,dados):criarParceria(dados));
  }
  function mudarEquipe(id:string){definirEquipe(id);definirResponsavel("");definirEvento("");definirProjeto("");definirDisponiveis({...opcoes,responsaveis:[],eventos:[],projetos:[]});definirCarregando(Boolean(id));}
  const referenciaAtual=(lista:{id:string;nome:string}[],id:string)=>id&&!lista.some(p=>p.id===id)?<option value={id}>Vínculo atual (histórico)</option>:null;
  return <Dialog open onClose={aoFechar} busy={ocupada} title={!inicial?"Nova parceria":modo==="arquivar"?"Arquivar parceria?":"Detalhes da parceria"}>
    {erro&&<ErrorState message={erro} onRetry={aoConcluir}/>}
    {modo==="ver"&&inicial&&<div className="space-y-4">
      <h3 className="break-words text-xl font-semibold">{inicial.organizacao}</h3><p className="subtle">{statusParceria[inicial.status]}{inicial.arquivada?" · Arquivada":""}</p>
      <p className="whitespace-pre-wrap break-words">{inicial.descricao||"Sem descricao."}</p>
      <dl className="grid gap-3 text-sm sm:grid-cols-2"><div><dt className="subtle">Equipe</dt><dd>{inicial.equipe.nome}</dd></div><div><dt className="subtle">Responsável</dt><dd>{inicial.responsavel?.nome||"Não definida"}</dd></div><div><dt className="subtle">Data da próxima ação</dt><dd>{inicial.dataProximaAcao?formatarData(inicial.dataProximaAcao):"Sem data"}</dd></div><div><dt className="subtle">Criado por</dt><dd>{inicial.criadaPor.nome}</dd></div></dl>
      <dl className="grid gap-3 text-sm sm:grid-cols-2">{[["Contato principal",inicial.contatoPrincipal],["E-mail",inicial.email],["Telefone",inicial.telefone],["Próxima ação",inicial.proximaAcao],["Observações",inicial.observacoes]].map(([nome,valor])=><div key={nome}><dt className="subtle">{nome}</dt><dd className="whitespace-pre-wrap break-words">{valor||"Não informado"}</dd></div>)}</dl>
      <div className="flex flex-wrap gap-3 text-sm">{inicial.eventoId&&inicial.capacidades.verEvento&&<Link className="text-primary underline" href={`/eventos?abrir=${inicial.eventoId}`}>Abrir evento vinculado</Link>}{inicial.projetoId&&inicial.capacidades.verProjeto&&<Link className="text-primary underline" href={`/projetos?abrir=${inicial.projetoId}`}>Abrir projeto vinculado</Link>}</div>
      <div className="flex flex-wrap gap-2">{inicial.capacidades.editar&&<Button onClick={()=>definirModo("editar")}>Editar parceria</Button>}{inicial.capacidades.arquivar&&<Button variant="secondary" onClick={()=>definirModo("arquivar")}>Arquivar parceria</Button>}</div>
    </div>}
    {modo==="editar"&&<form onSubmit={salvar} className="space-y-4">
      <label className="field">Organização<Input name="organizacao" defaultValue={inicial?.organizacao} maxLength={200} required disabled={ocupada}/></label>
      <label className="field">Descrição<textarea name="descricao" defaultValue={inicial?.descricao??""} maxLength={5000} rows={4} disabled={ocupada}/></label>
      {([ ["contatoPrincipal","Contato principal",200],["email","E-mail",254],["telefone","Telefone",50],["proximaAcao","Próxima ação",500],["observacoes","Observações",5000] ] as const).map(([campo,nome,limite])=><label className="field" key={campo}>{nome}{campo==="observacoes"?<textarea name={campo} defaultValue={inicial?.[campo]??""} maxLength={limite} rows={3} disabled={ocupada}/>:<Input name={campo} type={campo==="email"?"email":"text"} defaultValue={inicial?.[campo]??""} maxLength={limite} disabled={ocupada}/>}</label>)}
      <label className="field">Status<select name="status" defaultValue={inicial?.status??"PROSPECCAO"} disabled={ocupada}>{Object.entries(statusParceria).map(([v,n])=><option key={v} value={v}>{n}</option>)}</select></label>
      <label className="field">Equipe<select value={equipe} required disabled={ocupada} onChange={e=>mudarEquipe(e.target.value)}><option value="">Selecione</option>{opcoes.equipes.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
      <label className="field">Responsável<select value={responsavel} disabled={ocupada||!equipe||carregando} onChange={e=>definirResponsavel(e.target.value)}><option value="">Não definida</option>{disponiveis.responsaveis.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}{referenciaAtual(disponiveis.responsaveis,responsavel)}</select></label>
      <label className="field">Data da próxima ação<Input type="date" name="publicacao" defaultValue={inicial?.dataProximaAcao??""} disabled={ocupada}/></label>
      <label className="field">Evento vinculado<select value={evento} disabled={ocupada||!equipe||carregando} onChange={e=>definirEvento(e.target.value)}><option value="">Sem vínculo</option>{disponiveis.eventos.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}{referenciaAtual(disponiveis.eventos,evento)}</select></label>
      <label className="field">Projeto vinculado<select value={projeto} disabled={ocupada||!equipe||carregando} onChange={e=>definirProjeto(e.target.value)}><option value="">Sem vínculo</option>{disponiveis.projetos.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}{referenciaAtual(disponiveis.projetos,projeto)}</select></label>
      <div className="flex gap-2"><Button type="submit" disabled={ocupada||carregando||!equipe}>{ocupada?"Salvando…":"Salvar parceria"}</Button><Button variant="secondary" disabled={ocupada} onClick={()=>inicial?definirModo("ver"):aoFechar()}>Cancelar</Button></div>
    </form>}
    {modo==="arquivar"&&inicial&&<div className="space-y-4"><p>Arquivar “{inicial.organizacao}”? O registro permanece no histórico.</p><div className="flex gap-2"><Button variant="danger" disabled={ocupada} onClick={()=>void executar(()=>arquivarParceria(inicial))}>Confirmar arquivamento</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirModo("ver")}>Cancelar</Button></div></div>}
  </Dialog>;
}
