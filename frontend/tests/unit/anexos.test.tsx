// @vitest-environment jsdom
import { beforeEach,afterEach,it,expect,vi } from "vitest";
import {render,screen,fireEvent,waitFor,cleanup} from "@testing-library/react";
import {AbasColaboracao} from "../../src/components/comentarios/abas-colaboracao";
import {Anexos} from "../../src/components/anexos/anexos";
const api=vi.hoisted(()=>({listar:vi.fn(),iniciar:vi.fn(),confirmar:vi.fn(),enviar:vi.fn(),remover:vi.fn(),baixar:vi.fn(),receber:vi.fn()}));
vi.mock("../../src/lib/api/anexos",async original=>({...await original<object>(),listarAnexos:api.listar,iniciarAnexo:api.iniciar,confirmarAnexo:api.confirmar,enviarArquivo:api.enviar,removerAnexo:api.remover,baixarAnexo:api.baixar,receberArquivo:api.receber}));
const item={id:"anexo",nomeOriginal:"documento.pdf",tipoMime:"application/pdf",tamanhoBytes:20,enviadaPor:{id:"ana",nome:"Ana"},criadaEm:"2026-10-06T12:00:00Z",podeRemover:true};
const pagina={items:[item],total:1,page:0,size:25,podeAnexar:true};
beforeEach(()=>{vi.resetAllMocks();URL.createObjectURL=vi.fn(()=>"blob:validacao");URL.revokeObjectURL=vi.fn();api.receber.mockResolvedValue(new Blob(["arquivo"]));api.listar.mockResolvedValue(pagina);api.iniciar.mockResolvedValue({attachmentId:"novo",uploadUrl:"https://storage.example.test",headers:{}});api.enviar.mockResolvedValue(undefined);api.confirmar.mockResolvedValue(item);api.remover.mockResolvedValue(undefined);});
afterEach(cleanup);
function abrir(){render(<Anexos recurso="tasks" recursoId="tarefa"/>);}
async function selecionar(arquivo:File){fireEvent.change(await screen.findByLabelText("Selecionar arquivo"),{target:{files:[arquivo]}});}
it("mostra loading e lista",async()=>{abrir();expect(screen.getByRole("status")).toBeTruthy();expect(await screen.findByText("documento.pdf")).toBeTruthy();});
it("envia e confirma",async()=>{abrir();await selecionar(new File(["%PDF-1.7"],"novo.pdf"));await waitFor(()=>expect(api.confirmar).toHaveBeenCalledWith("tasks","tarefa","novo"));expect(await screen.findByText(/Arquivo anexado/)).toBeTruthy();});
it("recusa executavel",async()=>{abrir();await selecionar(new File(["MZ"],"a.exe"));expect(await screen.findByText(/Este tipo de arquivo não é permitido/)).toBeTruthy();expect(api.iniciar).not.toHaveBeenCalled();});
it("recusa tamanho excessivo",async()=>{abrir();await selecionar(new File([new Uint8Array(10485761)],"a.pdf"));expect(await screen.findByText(/ultrapassa o limite/)).toBeTruthy();expect(api.iniciar).not.toHaveBeenCalled();});
it("retry reutiliza registro",async()=>{api.enviar.mockRejectedValueOnce(new Error("Upload indisponível")).mockResolvedValueOnce(undefined);abrir();await selecionar(new File(["%PDF-1.7"],"a.pdf"));fireEvent.click(await screen.findByRole("button",{name:"Tentar novamente"}));await waitFor(()=>expect(api.confirmar).toHaveBeenCalled());expect(api.iniciar).toHaveBeenCalledTimes(1);expect(screen.getByText("documento.pdf")).toBeTruthy();});
it("retry de confirmacao nao reenvia bytes",async()=>{api.confirmar.mockRejectedValueOnce(new Error("Confirmar novamente")).mockResolvedValueOnce(item);abrir();await selecionar(new File(["%PDF-1.7"],"a.pdf"));fireEvent.click(await screen.findByRole("button",{name:"Tentar novamente"}));await waitFor(()=>expect(api.confirmar).toHaveBeenCalledTimes(2));expect(api.enviar).toHaveBeenCalledTimes(1);});
it("remove apos confirmar",async()=>{abrir();fireEvent.click(await screen.findByRole("button",{name:"Remover documento.pdf"}));expect(api.remover).not.toHaveBeenCalled();fireEvent.click(screen.getByRole("button",{name:"Confirmar remoção"}));await waitFor(()=>expect(api.remover).toHaveBeenCalledWith("anexo"));});
it("somente leitura esconde acoes",async()=>{api.listar.mockResolvedValue({...pagina,podeAnexar:false,items:[{...item,podeRemover:false}]});abrir();await screen.findByText("documento.pdf");expect(screen.queryByLabelText("Selecionar arquivo")).toBeNull();expect(screen.queryByRole("button",{name:"Remover documento.pdf"})).toBeNull();});
it("mostra vazio",async()=>{api.listar.mockResolvedValue({...pagina,items:[],total:0});abrir();expect(await screen.findByText(/Nenhum arquivo anexado/)).toBeTruthy();});
it("mostra erro",async()=>{api.listar.mockRejectedValue(new Error("Serviço indisponível"));abrir();expect(await screen.findByRole("alert")).toBeTruthy();});

for(const recurso of ["tasks","projects"] as const)it(`aba contextual de ${recurso}`,async()=>{
 render(<AbasColaboracao recurso={recurso} recursoId="recurso"><p>Detalhes</p></AbasColaboracao>);
 fireEvent.click(screen.getByRole("tab",{name:"Anexos"}));await screen.findByText("documento.pdf");
 expect(api.listar).toHaveBeenCalledWith(recurso,"recurso");
});
it("solicita URL de download antes de baixar",async()=>{
 api.baixar.mockResolvedValue({downloadUrl:"https://storage.example.test/download",expiresAt:"2026-10-06"});
 const clique=vi.spyOn(HTMLAnchorElement.prototype,"click").mockImplementation(()=>{});
 abrir();fireEvent.click(await screen.findByRole("button",{name:"Baixar documento.pdf"}));
 await waitFor(()=>expect(clique).toHaveBeenCalled());expect(api.baixar).toHaveBeenCalledWith("anexo");clique.mockRestore();
});
