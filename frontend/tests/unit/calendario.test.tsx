// @vitest-environment jsdom
import { beforeEach, afterEach, it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor, cleanup } from "@testing-library/react";
import { Calendario } from "../../src/components/calendario/calendario";
import { navegacaoPermitida } from "../../src/lib/ui/permissoes";
import type { ItemCalendario } from "../../src/lib/api/calendario";
const api = vi.hoisted(() => ({ calendario:vi.fn(), tarefa:vi.fn(), projeto:vi.fn(), opcoes:vi.fn() }));
vi.mock("../../src/lib/api/calendario", () => ({buscarCalendario:api.calendario}));
vi.mock("../../src/lib/api/tarefas", async original => ({...await original<object>(),buscarTarefa:api.tarefa,buscarOpcoesTarefas:api.opcoes}));
vi.mock("../../src/lib/api/projetos", async original => ({...await original<object>(),buscarProjeto:api.projeto,buscarOpcoesProjetos:api.opcoes}));
vi.mock("../../src/components/tarefas/detalhe-tarefa", () => ({DetalheTarefa:({inicial}:{inicial:{titulo:string}}) => <p>Detalhe reutilizado: {inicial.titulo}</p>}));
vi.mock("../../src/components/projetos/detalhe-projeto", () => ({DetalheProjeto:({inicial}:{inicial:{titulo:string}}) => <p>Detalhe reutilizado: {inicial.titulo}</p>}));
const equipe={id:"equipe",nome:"Comunicação"};
const tarefa:ItemCalendario={id:"TAREFA:tarefa",recursoId:"tarefa",tipo:"TAREFA",titulo:"Publicar campanha",dataInicio:"2026-10-07",dataFim:"2026-10-07",equipe,status:"A_FAZER",prioridade:"ALTA",responsaveis:[{id:"ana",nome:"Ana"}],concluido:false,atrasado:true};
const projeto:ItemCalendario={...tarefa,id:"PROJETO:projeto",recursoId:"projeto",tipo:"PROJETO",titulo:"Encontro TS",dataInicio:"2026-10-01",dataFim:"2026-10-22",status:"CONCLUIDO",prioridade:null,concluido:true,atrasado:false};
beforeEach(() => { vi.resetAllMocks();api.calendario.mockResolvedValue([tarefa,projeto]);api.opcoes.mockResolvedValue({equipes:[equipe],responsaveis:[{id:"ana",nome:"Ana"}]});api.tarefa.mockResolvedValue({titulo:tarefa.titulo,capacidades:{editar:false}});api.projeto.mockResolvedValue({titulo:projeto.titulo,capacidades:{editar:false}}); });
afterEach(cleanup);
function abrir(permissoes=["tasks.view","projects.view"],visao="agenda") {return render(<Calendario usuarioId="ana" permissoes={permissoes} equipes={[equipe]} dataInicial="2026-10-07" visaoInicial={visao}/>);}
it("sidebar usa união de permissões sem key própria", () => {
  for(const permissoes of [["tasks.view"],["projects.view"]]) expect(navegacaoPermitida(permissoes).some(p=>p.href==="/calendario")).toBe(true);
  expect(navegacaoPermitida([]).some(p=>p.href==="/calendario")).toBe(false);
});
it("agenda mostra datas, tipos, prioridade, status e período",async()=>{
  abrir();expect(await screen.findByRole("button",{name:"Abrir tarefa Publicar campanha"})).toBeTruthy();
  expect(screen.getByRole("button",{name:"Abrir projeto Encontro TS"})).toBeTruthy();
  expect(screen.getByText("Prioridade Alta")).toBeTruthy();expect(screen.getByText("Atrasado")).toBeTruthy();
  expect(screen.getByText(/Projeto · Concluído/)).toBeTruthy();
});
it("mês consulta janela limitada e permite selecionar dia vazio",async()=>{
  abrir(undefined,"month");await waitFor(()=>expect(api.calendario).toHaveBeenCalledWith("2026-09-28","2026-11-01","","",""));
  fireEvent.click(await screen.findByRole("button",{name:"27 de outubro de 2026"}));
  expect(screen.getByText("Nenhum item neste dia.")).toBeTruthy();
});
it("semana contém sete dias all-day sem horários",async()=>{
  abrir();fireEvent.click(screen.getByRole("button",{name:"Semana"}));
  await waitFor(()=>expect(api.calendario).toHaveBeenCalledWith("2026-10-05","2026-10-11","","",""));
  expect(await screen.findByRole("button",{name:"11 de outubro de 2026"})).toBeTruthy();expect(screen.queryByText("08:00")).toBeNull();
});
it("navega anterior/próximo e preserva estado na URL",async()=>{
  abrir();fireEvent.click(screen.getByRole("button",{name:"Próximo período"}));
  await waitFor(()=>expect(api.calendario).toHaveBeenLastCalledWith("2026-11-01","2026-11-30","","",""));
  expect(window.location.search).toContain("date=2026-11-01");
  fireEvent.click(screen.getByRole("button",{name:"Período anterior"}));
  await waitFor(()=>expect(api.calendario).toHaveBeenLastCalledWith("2026-10-01","2026-10-31","","",""));
});
it("Hoje retorna ao período corrente",async()=>{
  abrir();fireEvent.click(screen.getByRole("button",{name:"Hoje"}));
  await waitFor(()=>expect(api.calendario).toHaveBeenCalledTimes(2));expect(window.location.search).toContain("date=");
});
it("filtra equipe, tipo, responsável e meus itens no backend",async()=>{
  abrir();await screen.findByRole("button",{name:"Abrir tarefa Publicar campanha"});
  fireEvent.change(screen.getByLabelText("Equipe"),{target:{value:"equipe"}});
  await screen.findByRole("option",{name:"Ana"});
  fireEvent.change(screen.getByLabelText("Tipo"),{target:{value:"PROJETO"}});
  fireEvent.change(screen.getByLabelText("Responsável"),{target:{value:"ana"}});
  await waitFor(()=>expect(api.calendario).toHaveBeenLastCalledWith("2026-10-01","2026-10-31","equipe","PROJETO","ana"));
  fireEvent.click(screen.getByLabelText("Meus itens"));expect((screen.getByLabelText("Responsável") as HTMLSelectElement).disabled).toBe(true);
});
for(const [permissao,oculta] of [["tasks.view","Projetos"],["projects.view","Tarefas"]]) it(`filtros respeitam ${permissao}`,()=>{
  abrir([permissao]);expect(screen.queryByRole("option",{name:oculta})).toBeNull();
});
it("reutiliza detalhe de tarefa",async()=>{
  abrir();fireEvent.click(await screen.findByRole("button",{name:"Abrir tarefa Publicar campanha"}));
  expect(await screen.findByText("Detalhe reutilizado: Publicar campanha")).toBeTruthy();expect(api.tarefa).toHaveBeenCalledWith("tarefa");
});
it("reutiliza detalhe de projeto",async()=>{
  abrir();fireEvent.click(await screen.findByRole("button",{name:"Abrir projeto Encontro TS"}));
  expect(await screen.findByText("Detalhe reutilizado: Encontro TS")).toBeTruthy();expect(api.projeto).toHaveBeenCalledWith("projeto");
});
it("loading, vazio e erro recuperável",async()=>{
  api.calendario.mockResolvedValue([]);abrir();expect(screen.getByRole("status",{name:"Carregando calendário"})).toBeTruthy();
  expect(await screen.findByText("Nenhum item neste período.")).toBeTruthy();
});
it("erro seguro preserva controles e permite retry",async()=>{
  api.calendario.mockRejectedValueOnce(new Error("Serviço indisponível"));abrir();
  expect(await screen.findByRole("alert")).toBeTruthy();fireEvent.click(screen.getByRole("button",{name:"Tentar novamente"}));
  expect(await screen.findByRole("button",{name:"Abrir tarefa Publicar campanha"})).toBeTruthy();
});
