// @vitest-environment jsdom
import { it, expect, vi, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import CalendarioPage from "../../src/app/calendario/page";
const api=vi.hoisted(()=>({sessao:vi.fn(),ler:vi.fn()}));
vi.mock("../../src/lib/sessao",()=>({exigirSessao:api.sessao}));
vi.mock("../../src/lib/api/server",()=>({lerJava:api.ler}));
vi.mock("../../src/components/layout/shell-workspace",()=>({ShellWorkspace:({children}:{children:React.ReactNode})=><div>{children}</div>}));
vi.mock("../../src/components/calendario/calendario",()=>({Calendario:({dataInicial,visaoInicial}:{dataInicial:string;visaoInicial:string})=><p>Calendário {visaoInicial} {dataInicial}</p>}));
afterEach(()=>{cleanup();vi.resetAllMocks();});
for(const [permissao,rota] of [["tasks.view","/tasks/options"],["projects.view","/projects/options"]]) it(`rota consulta apenas fonte autorizada ${permissao}`,async()=>{
 api.sessao.mockResolvedValue({id:"ana",permissions:[permissao]});api.ler.mockResolvedValue({equipes:[],responsaveis:[]});
 render(await CalendarioPage({searchParams:Promise.resolve({date:"2026-10-07",view:"agenda"})}));
 expect(screen.getByText("Calendário agenda 2026-10-07")).toBeTruthy();expect(api.ler).toHaveBeenCalledWith(rota);
 expect(api.ler).toHaveBeenCalledTimes(1);
});
it("rota sem ambas as permissões não consulta dados",async()=>{
 api.sessao.mockResolvedValue({id:"ana",permissions:[]});
 render(await CalendarioPage({searchParams:Promise.resolve({})}));
 expect(screen.getByRole("alert")).toBeTruthy();expect(api.ler).not.toHaveBeenCalled();
});
