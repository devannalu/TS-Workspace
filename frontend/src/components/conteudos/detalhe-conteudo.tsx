"use client";
import { useEffect,useState,type FormEvent } from "react";
import Link from "next/link";
import { criarConteudo,editarConteudo,arquivarConteudo,opcoesConteudos,statusConteudo,canaisConteudo,formatosConteudo,type DadosConteudo,type Conteudo,type OpcoesConteudos } from "@/lib/api/conteudos";
import { Dialog } from "../ui/dialog";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { ErrorState } from "../ui/feedback";
import { formatarData } from "../calendario/datas";
export function DetalheConteudo({inicial,opcoes,aoFechar,aoConcluir}:{inicial:Conteudo|null;opcoes:OpcoesConteudos;aoFechar:()=>void;aoConcluir:()=>void}) {
  const [modo,definirModo]=useState<"ver"|"editar"|"arquivar">(inicial?"ver":"editar");
  const [ocupada,definirOcupada]=useState(false),[erro,definirErro]=useState("");
  const [equipe,definirEquipe]=useState(inicial?.equipe.id??""),[disponiveis,definirDisponiveis]=useState(opcoes);
  const [responsavel,definirResponsavel]=useState(inicial?.responsavel?.id??"");
  const [evento,definirEvento]=useState(inicial?.eventoId??""),[projeto,definirProjeto]=useState(inicial?.projetoId??"");
  const [carregando,definirCarregando]=useState(false);
  useEffect(()=>{if(!equipe)return;let ativa=true;opcoesConteudos(equipe).then(o=>{if(ativa){definirDisponiveis(o);definirCarregando(false);}}).catch(e=>{if(ativa){definirErro(e instanceof Error?e.message:"Não foi possível carregar opções.");definirCarregando(false);}});return()=>{ativa=false;};},[equipe]);
  async function executar(acao:()=>Promise<unknown>) {definirOcupada(true);definirErro("");try{await acao();aoConcluir();}catch(e){definirErro(e instanceof Error?e.message:"Não foi possível concluir a operação.");}finally{definirOcupada(false);}}
  function salvar(e:FormEvent<HTMLFormElement>) {
    e.preventDefault();const f=new FormData(e.currentTarget);
    const dados:DadosConteudo={titulo:String(f.get("titulo")),briefing:String(f.get("briefing")),canal:String(f.get("canal")) as Conteudo["canal"],formato:String(f.get("formato")) as Conteudo["formato"],status:String(f.get("status")) as Conteudo["status"],equipeId:equipe,responsavelId:responsavel||null,publicacaoPlanejada:String(f.get("publicacao"))||null,eventoId:evento||null,projetoId:projeto||null};
    void executar(()=>inicial?editarConteudo(inicial,dados):criarConteudo(dados));
  }
  function mudarEquipe(id:string){definirEquipe(id);definirResponsavel("");definirEvento("");definirProjeto("");definirDisponiveis({...opcoes,responsaveis:[],eventos:[],projetos:[]});definirCarregando(Boolean(id));}
  const referenciaAtual=(lista:{id:string;nome:string}[],id:string)=>id&&!lista.some(p=>p.id===id)?<option value={id}>Vínculo atual (histórico)</option>:null;
  return <Dialog open onClose={aoFechar} busy={ocupada} title={!inicial?"Novo conteúdo":modo==="arquivar"?"Arquivar conteúdo?":"Detalhes do conteúdo"}>
    {erro&&<ErrorState message={erro} onRetry={aoConcluir}/>}
    {modo==="ver"&&inicial&&<div className="space-y-4">
      <h3 className="break-words text-xl font-semibold">{inicial.titulo}</h3><p className="subtle">{canaisConteudo[inicial.canal]} · {formatosConteudo[inicial.formato]} · {statusConteudo[inicial.status]}{inicial.arquivada?" · Arquivado":""}</p>
      <p className="whitespace-pre-wrap break-words">{inicial.briefing||"Sem briefing."}</p>
      <dl className="grid gap-3 text-sm sm:grid-cols-2"><div><dt className="subtle">Equipe</dt><dd>{inicial.equipe.nome}</dd></div><div><dt className="subtle">Responsável</dt><dd>{inicial.responsavel?.nome||"Não definida"}</dd></div><div><dt className="subtle">Publicação planejada</dt><dd>{inicial.publicacaoPlanejada?formatarData(inicial.publicacaoPlanejada):"Sem data"}</dd></div><div><dt className="subtle">Criado por</dt><dd>{inicial.criadaPor.nome}</dd></div></dl>
      <div className="flex flex-wrap gap-3 text-sm">{inicial.eventoId&&inicial.capacidades.verEvento&&<Link className="text-primary underline" href={`/eventos?abrir=${inicial.eventoId}`}>Abrir evento vinculado</Link>}{inicial.projetoId&&inicial.capacidades.verProjeto&&<Link className="text-primary underline" href={`/projetos?abrir=${inicial.projetoId}`}>Abrir projeto vinculado</Link>}</div>
      <div className="flex flex-wrap gap-2">{inicial.capacidades.editar&&<Button onClick={()=>definirModo("editar")}>Editar conteúdo</Button>}{inicial.capacidades.arquivar&&<Button variant="secondary" onClick={()=>definirModo("arquivar")}>Arquivar conteúdo</Button>}</div>
    </div>}
    {modo==="editar"&&<form onSubmit={salvar} className="space-y-4">
      <label className="field">Título<Input name="titulo" defaultValue={inicial?.titulo} maxLength={200} required disabled={ocupada}/></label>
      <label className="field">Briefing<textarea name="briefing" defaultValue={inicial?.briefing??""} maxLength={5000} rows={4} disabled={ocupada}/></label>
      <div className="grid gap-3 sm:grid-cols-2"><label className="field">Canal<select name="canal" defaultValue={inicial?.canal??"INSTAGRAM"} disabled={ocupada}>{Object.entries(canaisConteudo).map(([v,n])=><option key={v} value={v}>{n}</option>)}</select></label><label className="field">Formato<select name="formato" defaultValue={inicial?.formato??"POST"} disabled={ocupada}>{Object.entries(formatosConteudo).map(([v,n])=><option key={v} value={v}>{n}</option>)}</select></label></div>
      <label className="field">Status<select name="status" defaultValue={inicial?.status??"IDEIA"} disabled={ocupada}>{Object.entries(statusConteudo).map(([v,n])=><option key={v} value={v}>{n}</option>)}</select></label>
      <label className="field">Equipe<select value={equipe} required disabled={ocupada} onChange={e=>mudarEquipe(e.target.value)}><option value="">Selecione</option>{opcoes.equipes.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}</select></label>
      <label className="field">Responsável<select value={responsavel} disabled={ocupada||!equipe||carregando} onChange={e=>definirResponsavel(e.target.value)}><option value="">Não definida</option>{disponiveis.responsaveis.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}{referenciaAtual(disponiveis.responsaveis,responsavel)}</select></label>
      <label className="field">Publicação planejada<Input type="date" name="publicacao" defaultValue={inicial?.publicacaoPlanejada??""} disabled={ocupada}/></label>
      <label className="field">Evento vinculado<select value={evento} disabled={ocupada||!equipe||carregando} onChange={e=>definirEvento(e.target.value)}><option value="">Sem vínculo</option>{disponiveis.eventos.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}{referenciaAtual(disponiveis.eventos,evento)}</select></label>
      <label className="field">Projeto vinculado<select value={projeto} disabled={ocupada||!equipe||carregando} onChange={e=>definirProjeto(e.target.value)}><option value="">Sem vínculo</option>{disponiveis.projetos.map(p=><option key={p.id} value={p.id}>{p.nome}</option>)}{referenciaAtual(disponiveis.projetos,projeto)}</select></label>
      <div className="flex gap-2"><Button type="submit" disabled={ocupada||carregando||!equipe}>{ocupada?"Salvando…":"Salvar conteúdo"}</Button><Button variant="secondary" disabled={ocupada} onClick={()=>inicial?definirModo("ver"):aoFechar()}>Cancelar</Button></div>
    </form>}
    {modo==="arquivar"&&inicial&&<div className="space-y-4"><p>Arquivar “{inicial.titulo}”? O registro permanece no histórico.</p><div className="flex gap-2"><Button variant="danger" disabled={ocupada} onClick={()=>void executar(()=>arquivarConteudo(inicial))}>Confirmar arquivamento</Button><Button variant="secondary" disabled={ocupada} onClick={()=>definirModo("ver")}>Cancelar</Button></div></div>}
  </Dialog>;
}
