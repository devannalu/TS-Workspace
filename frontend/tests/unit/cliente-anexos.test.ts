import {it,expect,vi,afterEach} from "vitest";
import {enviarArquivo,receberArquivo} from "../../src/lib/api/anexos";
afterEach(()=>vi.unstubAllGlobals());
it("upload e download diretos omitem cookies",async()=>{
 const fetchArquivo=vi.fn().mockResolvedValue({ok:true,blob:async()=>new Blob(["arquivo"])});vi.stubGlobal("fetch",fetchArquivo);
 await enviarArquivo(new File(["%PDF-1.7"],"a.pdf"),{attachmentId:"id",uploadUrl:"https://storage.example.test/upload",headers:{"Content-Type":"application/pdf"},expiresAt:"2026-10-06"});
 await receberArquivo("https://storage.example.test/download");
 expect(fetchArquivo.mock.calls.every((chamada)=>chamada[1].credentials==="omit")).toBe(true);
});
