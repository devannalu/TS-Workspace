import { requisitarJava } from "./http";
export type RecursoAnexo = "tasks" | "projects";
export type Anexo = { id: string; nomeOriginal: string; tipoMime: string; tamanhoBytes: number;
  enviadaPor: { id: string; nome: string }; criadaEm: string; podeRemover: boolean };
export type PaginaAnexos = { items: Anexo[]; total: number; page: number; size: number; podeAnexar: boolean };
export type UploadAnexo = { attachmentId: string; uploadUrl: string; headers: Record<string,string>; expiresAt: string };
export const listarAnexos = (recurso: RecursoAnexo,id: string,page=0) =>
  requisitarJava<PaginaAnexos>(`/${recurso}/${id}/attachments?page=${page}&size=25`);
export const iniciarAnexo = (recurso: RecursoAnexo,id: string,arquivo: File,solicitacaoId: string) =>
  requisitarJava<UploadAnexo>(`/${recurso}/${id}/attachments/upload`,"POST",{
    solicitacaoId,nomeOriginal: arquivo.name,tipoMime: tipoArquivo(arquivo),tamanhoBytes: arquivo.size });
export const confirmarAnexo = (recurso: RecursoAnexo,id: string,attachmentId: string) =>
  requisitarJava<Anexo>(`/${recurso}/${id}/attachments/${attachmentId}/confirm`,"POST");
export const removerAnexo = (id: string) => requisitarJava<void>(`/attachments/${id}/remove`,"POST");
export const baixarAnexo = (id: string) => requisitarJava<{ downloadUrl: string; expiresAt: string }>(`/attachments/${id}/download`,"POST");
const tipos: Record<string,string> = {pdf:"application/pdf",png:"image/png",jpg:"image/jpeg",jpeg:"image/jpeg",
  webp:"image/webp",txt:"text/plain",csv:"text/csv"};
export function tipoArquivo(arquivo: File) { return tipos[arquivo.name.split(".").pop()?.toLowerCase() ?? ""] ?? ""; }
export function validarArquivo(arquivo: File) {
  if(!tipoArquivo(arquivo))return "Este tipo de arquivo não é permitido.";
  if(arquivo.size>10*1024*1024)return "O arquivo ultrapassa o limite de 10 MB.";
  if(!arquivo.size)return "O arquivo está vazio.";
  return null;
}
export async function enviarArquivo(arquivo: File,upload: UploadAnexo) {
  const resposta=await fetch(upload.uploadUrl,{method:"PUT",headers:upload.headers,body:arquivo,credentials:"omit"}).catch(()=>{throw new Error("Não foi possível enviar o arquivo. Tente novamente.");});
  if(!resposta.ok)throw new Error("Não foi possível enviar o arquivo. Tente novamente.");
}

export async function receberArquivo(downloadUrl: string) {
  const resposta=await fetch(downloadUrl,{credentials:"omit"}).catch(()=>{throw new Error("Não foi possível baixar o arquivo. Tente novamente.");});
  if(!resposta.ok)throw new Error("Não foi possível baixar o arquivo. Tente novamente.");
  return resposta.blob();
}
