// @vitest-environment jsdom
import {beforeEach,afterEach,it,expect,vi} from "vitest";
import {render,screen,fireEvent,cleanup} from "@testing-library/react";
import {FormularioConvite} from "../../src/components/convites/formulario-convite";
const api=vi.hoisted(()=>({criar:vi.fn(),refresh:vi.fn()}));
vi.mock("next/navigation",()=>({useRouter:()=>({refresh:api.refresh})}));
vi.mock("../../src/lib/api/acoes",()=>({criarConviteComFeedback:api.criar}));
beforeEach(()=>{vi.clearAllMocks();HTMLDialogElement.prototype.showModal=function(){this.setAttribute("open","");};HTMLDialogElement.prototype.close=function(){this.removeAttribute("open");};});afterEach(cleanup);
it.each([
 ["ENVIADO","Convite enviado por e-mail. Você também pode copiar o link abaixo."],
 ["FALHOU","Convite criado, mas não foi possível enviar o e-mail. Copie o link e compartilhe com a pessoa convidada."],
 ["DESATIVADO","Envio por e-mail desativado. Copie o link e compartilhe com a pessoa convidada."],
])("informa %s sem perder o link de uso único",async(envioEmail,mensagem)=>{
 api.criar.mockResolvedValue({ok:true,url:"http://localhost:3010/convite/fixture",envioEmail});
 render(<FormularioConvite perfisAcesso={[{id:"suporte",name:"Suporte"}]} equipes={[{id:"equipe",name:"Equipe exemplo"}]} inicialmenteAberto/>);
 fireEvent.change(screen.getByRole("textbox",{name:"E-mail"}),{target:{value:"integrante@example.test"}});
 fireEvent.change(screen.getByLabelText("Perfil de acesso"),{target:{value:"suporte"}});
 fireEvent.click(screen.getByRole("checkbox",{name:"Equipe exemplo"}));
 fireEvent.click(screen.getByRole("button",{name:"Criar convite"}));
 expect(await screen.findByText(mensagem)).toBeTruthy();expect((screen.getByLabelText("Link de convite") as HTMLInputElement).value).toBe("http://localhost:3010/convite/fixture");
 expect(api.criar).toHaveBeenCalledTimes(1);
});
