// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { CentralNotificacoes } from "../../src/components/notificacoes/central-notificacoes";
const api=vi.hoisted(()=>({listar:vi.fn(),ler:vi.fn(),todas:vi.fn()}));
vi.mock("../../src/lib/api/notificacoes",()=>({listarNotificacoes:api.listar,lerNotificacao:api.ler,lerTodasNotificacoes:api.todas}));
const item={id:"n",tipo:"TAREFA",recursoId:"t",titulo:"Preparar reunião",motivo:"ATRIBUICAO",criadaEm:"2026-10-07T12:00:00Z",lida:false};
beforeEach(()=>{
 vi.resetAllMocks();Object.defineProperty(document,"visibilityState",{configurable:true,value:"visible"});
 api.listar.mockResolvedValue({items:[item],total:1,naoLidas:1,page:0,size:20});
 Object.defineProperty(HTMLDialogElement.prototype,"showModal",{configurable:true,value:function(){this.open=true;}});
 Object.defineProperty(HTMLDialogElement.prototype,"close",{configurable:true,value:function(){this.open=false;}});
});
afterEach(cleanup);
it("mostra contador, motivo e link para detalhe autorizado",async()=>{
 render(<CentralNotificacoes/>);fireEvent.click(await screen.findByRole("button",{name:"Notificações, 1 não lidas"}));
 expect(screen.getByRole("link",{name:"Preparar reunião"}).getAttribute("href")).toBe("/tarefas?abrir=t");
 expect(screen.getByText(/Você foi adicionada como responsável/)).toBeTruthy();
});
it("marca uma e recarrega sem marcar outra",async()=>{
 render(<CentralNotificacoes/>);fireEvent.click(await screen.findByRole("button",{name:"Notificações, 1 não lidas"}));
 fireEvent.click(screen.getByRole("button",{name:"Marcar como lida"}));
 await waitFor(()=>expect(api.ler).toHaveBeenCalledWith("n"));expect(api.todas).not.toHaveBeenCalled();
});
it("marca todas por ação explícita",async()=>{
 render(<CentralNotificacoes/>);fireEvent.click(await screen.findByRole("button",{name:"Notificações, 1 não lidas"}));
 fireEvent.click(screen.getByRole("button",{name:"Marcar todas como lidas"}));await waitFor(()=>expect(api.todas).toHaveBeenCalledOnce());
});
it("vazio e erro têm feedback e retry",async()=>{
 api.listar.mockRejectedValueOnce(new Error("Serviço indisponível"));render(<CentralNotificacoes/>);
 await waitFor(()=>expect(api.listar).toHaveBeenCalled());fireEvent.click(screen.getByRole("button",{name:"Notificações"}));
 await screen.findByRole("link",{name:"Preparar reunião"});
 api.listar.mockResolvedValue({items:[],total:0,naoLidas:0,page:0,size:20});
 fireEvent.click(screen.getByRole("button",{name:"Marcar todas como lidas"}));expect(await screen.findByText("Tudo em dia.")).toBeTruthy();
});
it("paginação solicita somente a próxima página",async()=>{
 api.listar.mockResolvedValue({items:[item],total:21,naoLidas:1,page:0,size:20});render(<CentralNotificacoes/>);
 fireEvent.click(await screen.findByRole("button",{name:"Notificações, 1 não lidas"}));fireEvent.click(screen.getByRole("button",{name:"Próxima"}));
 await waitFor(()=>expect(api.listar).toHaveBeenCalledWith(1));
});

it("erro visível pode ser recuperado por retry",async()=>{
 api.listar.mockRejectedValue(new Error("Serviço indisponível"));render(<CentralNotificacoes/>);
 fireEvent.click(screen.getByRole("button",{name:"Notificações"}));
 expect(await screen.findByRole("alert")).toBeTruthy();
 api.listar.mockResolvedValue({items:[],total:0,naoLidas:0,page:0,size:20});
 fireEvent.click(screen.getByRole("button",{name:"Tentar novamente"}));expect(await screen.findByText("Tudo em dia.")).toBeTruthy();
});
