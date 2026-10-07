"use client";
import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Bell } from "lucide-react";
import { listarNotificacoes, lerNotificacao, lerTodasNotificacoes, type PaginaNotificacoes } from "@/lib/api/notificacoes";
import { Dialog } from "../ui/dialog";
import { Button } from "../ui/button";
import { Pagination } from "../ui/pagination";
import { EmptyState, ErrorState } from "../ui/feedback";
const motivos:Record<string,string> = { ATRIBUICAO:"Você foi adicionada como responsável",COMENTARIO:"Novo comentário",ALTERACAO:"Status ou prazo atualizado",PRAZO:"Prazo próximo",ATRASO:"Prazo atrasado" };
export function CentralNotificacoes() {
  const [aberta,definirAberta]=useState(false), [pagina,definirPagina]=useState(0);
  const [dados,definirDados]=useState<PaginaNotificacoes>(), [erro,definirErro]=useState(""), [ocupada,definirOcupada]=useState(false);
  const carregar=useCallback(async()=>{
    try { const resposta=await listarNotificacoes(pagina); definirDados(resposta);definirErro(""); }
    catch(e) { definirErro(e instanceof Error?e.message:"Não foi possível carregar notificações."); }
  },[pagina]);
  useEffect(()=>{
    let ativa=true;
    const consultar=async()=>{ if(document.visibilityState!=="visible")return; try { const resposta=await listarNotificacoes(pagina);if(ativa){definirDados(resposta);definirErro("");} }
      catch(e){if(ativa)definirErro(e instanceof Error?e.message:"Não foi possível carregar notificações.");} };
    void consultar(); const intervalo=setInterval(()=>void consultar(),60000);
    document.addEventListener("visibilitychange",consultar);
    return()=>{ativa=false;clearInterval(intervalo);document.removeEventListener("visibilitychange",consultar);};
  },[pagina]);
  async function marcar(id?:string) {
    definirOcupada(true);
    try { if(id)await lerNotificacao(id);else await lerTodasNotificacoes();await carregar(); }
    catch(e){definirErro(e instanceof Error?e.message:"Não foi possível atualizar notificações.");}
    finally{definirOcupada(false);}
  }
  return <>
    <button type="button" aria-label={`Notificações${dados?.naoLidas?`, ${dados.naoLidas} não lidas`:""}`} className="relative flex size-11 items-center justify-center rounded-xl hover:bg-muted"
      onClick={()=>{definirAberta(true);void carregar();}}><Bell size={20} aria-hidden />{!!dados?.naoLidas&&<span className="absolute right-0 top-0 rounded-full bg-primary px-1 text-xs text-primary-foreground">{dados.naoLidas>99?"99+":dados.naoLidas}</span>}</button>
    <Dialog open={aberta} onClose={()=>definirAberta(false)} title="Notificações" description="Atribuições, comentários e prazos que merecem sua atenção.">
      <div className="space-y-4">
        {erro&&<ErrorState message={erro} onRetry={()=>void carregar()} />}
        {!dados&&!erro&&<p role="status">Carregando notificações…</p>}
        {dados&&<><Button variant="secondary" disabled={ocupada||!dados.naoLidas} onClick={()=>void marcar()}>Marcar todas como lidas</Button>
          {!dados.items.length&&<EmptyState title="Tudo em dia." description="Seus avisos aparecerão aqui." />}
          <ul className="space-y-3">{dados.items.map(n=><li key={n.id} className={`rounded-xl border border-border p-3 ${n.lida?"":"bg-accent"}`}>
            <p className="text-xs text-muted-foreground">{motivos[n.motivo]} · {n.lida?"Lida":"Não lida"}</p>
            <Link className="mt-1 block break-words font-medium" href={`/${n.tipo==="TAREFA"?"tarefas":"projetos"}?abrir=${n.recursoId}`} onClick={()=>definirAberta(false)}>{n.titulo}</Link>
            <time className="mt-1 block text-xs text-muted-foreground" dateTime={n.criadaEm}>{new Date(n.criadaEm).toLocaleString("pt-BR")}</time>
            {!n.lida&&<Button variant="ghost" disabled={ocupada} onClick={()=>void marcar(n.id)}>Marcar como lida</Button>}
          </li>)}</ul>
          <Pagination page={dados.page} size={dados.size} total={dados.total} onPage={definirPagina} label="notificações" />
        </>}
      </div>
    </Dialog>
  </>;
}
