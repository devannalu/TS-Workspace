"use client";
import { useEffect,useRef,useState } from "react";
import { FileText,Download,Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { listarAnexos,iniciarAnexo,confirmarAnexo,enviarArquivo,removerAnexo,baixarAnexo,receberArquivo,validarArquivo,
  type RecursoAnexo,type PaginaAnexos,type UploadAnexo } from "@/lib/api/anexos";
import { formatarDataColaboracao } from "@/lib/api/comentarios";

type Envio = { arquivo: File; solicitacaoId: string; upload?: UploadAnexo; enviado: boolean; estado: "aguardando"|"enviando"|"confirmando"|"concluido"|"erro"; erro?: string };
export function Anexos({recurso,recursoId}:{recurso:RecursoAnexo;recursoId:string}) {
  const [pagina,definirPagina]=useState<PaginaAnexos>();
  const [carregando,definirCarregando]=useState(true);
  const [erro,definirErro]=useState("");
  const [envio,definirEnvio]=useState<Envio>();
  const [remocao,definirRemocao]=useState<string>();
  const [ocupado,definirOcupado]=useState(false);
  const entrada=useRef<HTMLInputElement>(null);
  const botaoRemocao=useRef<HTMLButtonElement|null>(null);
  async function carregar(page=0) {
    definirCarregando(true);definirErro("");
    try {definirPagina(await listarAnexos(recurso,recursoId,page));}
    catch(e) {definirErro(e instanceof Error?e.message:"Não foi possível carregar os anexos.");}
    finally {definirCarregando(false);}
  }
  useEffect(()=>{
    let ativo=true;
    listarAnexos(recurso,recursoId).then(p=>{if(ativo)definirPagina(p);})
      .catch(e=>{if(ativo)definirErro(e instanceof Error?e.message:"Não foi possível carregar os anexos.");})
      .finally(()=>{if(ativo)definirCarregando(false);});
    return ()=>{ativo=false;};
  },[recurso,recursoId]);
  async function enviar(atual:Envio) {
    definirOcupado(true);definirErro("");
    try {
      atual={...atual,estado:"aguardando",erro:undefined};definirEnvio(atual);
      if(!atual.upload)atual={...atual,upload:await iniciarAnexo(recurso,recursoId,atual.arquivo,atual.solicitacaoId)};
      definirEnvio(atual);
      if(!atual.enviado) {
        atual={...atual,estado:"enviando"};definirEnvio(atual);
        await enviarArquivo(atual.arquivo,atual.upload!);atual={...atual,enviado:true};
      }
      atual={...atual,estado:"confirmando"};definirEnvio(atual);
      await confirmarAnexo(recurso,recursoId,atual.upload!.attachmentId);
      definirEnvio({...atual,estado:"concluido"});if(entrada.current)entrada.current.value="";
      await carregar();
    } catch(e) {if(entrada.current)entrada.current.value="";definirEnvio({...atual,estado:"erro",erro:e instanceof Error?e.message:"Não foi possível enviar o arquivo."});}
    finally {definirOcupado(false);}
  }
  async function baixar(id:string,nome:string) {
    definirOcupado(true);definirErro("");
    try {
      const resposta=await baixarAnexo(id);const arquivo=await receberArquivo(resposta.downloadUrl);
      const url=URL.createObjectURL(arquivo),link=document.createElement("a");link.href=url;link.download=nome;document.body.appendChild(link);link.click();link.remove();
      setTimeout(()=>URL.revokeObjectURL(url),1000);
    }
    catch(e){definirErro(e instanceof Error?e.message:"Não foi possível baixar o arquivo.");}
    finally{definirOcupado(false);}
  }
  async function remover(id:string) {
    definirOcupado(true);definirErro("");
    try{await removerAnexo(id);definirRemocao(undefined);await carregar(pagina?.page??0);entrada.current?.focus();}
    catch(e){definirErro(e instanceof Error?e.message:"Não foi possível remover o arquivo. Tente novamente para concluir a limpeza.");}
    finally{definirOcupado(false);}
  }
  return <section aria-label="Anexos" className="space-y-4">
    {pagina?.podeAnexar?<div className="space-y-2 rounded-lg border border-border p-3">
      <label className="block text-sm font-medium" htmlFor={`arquivo-${recursoId}`}>Selecionar arquivo</label>
      <input ref={entrada} id={`arquivo-${recursoId}`} type="file" accept=".pdf,.png,.jpg,.jpeg,.webp,.txt,.csv" disabled={ocupado}
        className="block w-full min-w-0 text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-muted file:px-3 file:py-2"
        onChange={e=>{const arquivo=e.target.files?.[0];if(!arquivo)return;const falha=validarArquivo(arquivo);
          if(falha){definirEnvio({arquivo,solicitacaoId:crypto.randomUUID(),enviado:false,estado:"erro",erro:falha});return;}
          void enviar({arquivo,solicitacaoId:crypto.randomUUID(),enviado:false,estado:"aguardando"});}} />
      <p className="subtle text-xs">PDF, PNG, JPEG, WEBP, TXT e CSV. Até 10 MB por arquivo.</p>
    </div>:pagina?<p className="subtle text-sm">Anexos disponíveis somente para leitura.</p>:null}
    {envio&&<div aria-live="polite" className="text-sm">
      <p className="break-words">{envio.arquivo.name}: {envio.estado==="aguardando"?"Aguardando autorização…":envio.estado==="enviando"?"Enviando…":envio.estado==="confirmando"?"Confirmando…":envio.estado==="concluido"?"Arquivo anexado.":envio.erro}</p>
      {envio.estado==="erro"&&!validarArquivo(envio.arquivo)&&<Button disabled={ocupado} onClick={()=>void enviar(envio)}>Tentar novamente</Button>}
    </div>}
    {erro&&<div role="alert" className="space-y-2 text-sm"><p>{erro}</p><Button disabled={ocupado} onClick={()=>void carregar(pagina?.page??0)}>Atualizar anexos</Button></div>}
    {carregando?<p role="status" className="subtle text-sm">Carregando anexos…</p>:pagina?.items.length===0?<p className="subtle text-sm">Nenhum arquivo anexado.{pagina.podeAnexar?" Adicione arquivos importantes para este recurso.":""}</p>:null}
    <ul className="divide-y divide-border">
      {pagina?.items.map(a=><li key={a.id} className="flex min-w-0 flex-wrap items-center gap-2 py-3">
        <FileText size={18} aria-hidden="true" className="shrink-0" />
        <div className="min-w-0 flex-1 basis-40"><p className="truncate text-sm font-medium" title={a.nomeOriginal}>{a.nomeOriginal}</p>
          <p className="subtle text-xs">{a.tipoMime} · {a.tamanhoBytes<1024 ? `${a.tamanhoBytes} bytes` : `${(a.tamanhoBytes/1024).toLocaleString("pt-BR",{maximumFractionDigits:1})} KB`}</p>
          <p className="subtle break-words text-xs">{a.enviadaPor.nome} · {formatarDataColaboracao(a.criadaEm)}</p></div>
        <Button disabled={ocupado} aria-label={`Baixar ${a.nomeOriginal}`} onClick={()=>void baixar(a.id,a.nomeOriginal)}><Download size={16} aria-hidden="true" /></Button>
        {a.podeRemover&&<Button disabled={ocupado} aria-label={`Remover ${a.nomeOriginal}`} onClick={e=>{botaoRemocao.current=e.currentTarget;definirRemocao(a.id);}}><Trash2 size={16} aria-hidden="true" /></Button>}
        {remocao===a.id&&<div role="group" aria-label="Confirmar remoção" className="flex w-full flex-wrap items-center gap-2 text-sm">
          <p>Remover este arquivo?</p><Button disabled={ocupado} onClick={()=>void remover(a.id)}>Confirmar remoção</Button>
          <Button disabled={ocupado} onClick={()=>{definirRemocao(undefined);botaoRemocao.current?.focus();}}>Cancelar</Button></div>}
      </li>)}
    </ul>
    {pagina&&pagina.total>25&&<div className="flex items-center gap-2"><Button disabled={carregando||pagina.page===0} onClick={()=>void carregar(pagina.page-1)}>Anterior</Button>
      <span className="text-sm">Página {pagina.page+1}</span><Button disabled={carregando||(pagina.page+1)*25>=pagina.total} onClick={()=>void carregar(pagina.page+1)}>Próxima</Button></div>}
  </section>;
}
