// @vitest-environment jsdom
import { beforeEach, afterEach, it, expect, vi, type Mock } from "vitest";
import {
  render,
  screen,
  cleanup,
  fireEvent,
  waitFor,
} from "@testing-library/react";
import type { ReactNode } from "react";
import { FormularioTarefa } from "../../src/components/tarefas/formulario-tarefa";
import { QuadroTarefas } from "../../src/components/tarefas/quadro-tarefas";
import PainelPage from "../../src/app/workspace/page";
const api = vi.hoisted(() => ({
  tarefas: vi.fn(),
  opcoes: vi.fn(),
  projetos: vi.fn(),
  ler: vi.fn(),
  sessao: vi.fn(),
}));
vi.mock("../../src/lib/api/tarefas", async (original) => ({
  ...(await original<object>()),
  listarTarefas: api.tarefas,
  buscarOpcoesTarefas: api.opcoes,
}));
vi.mock("../../src/lib/api/projetos", async (original) => ({
  ...(await original<object>()),
  listarProjetos: api.projetos,
}));
vi.mock("../../src/lib/api/server", () => ({ lerJava: api.ler }));
vi.mock("../../src/lib/sessao", () => ({
  exigirSessao: api.sessao,
  buscarMinhasEquipes: async () => [],
}));
vi.mock("../../src/components/layout/shell-workspace", () => ({
  ShellWorkspace: ({ children }: { children: ReactNode }) => (
    <div>{children}</div>
  ),
}));
const opcoes = {
  equipes: [
    { id: "equipe", nome: "Comunicação" },
    { id: "outra", nome: "Eventos" },
  ],
  responsaveis: [],
};
beforeEach(() => {
  vi.resetAllMocks();
  api.opcoes.mockResolvedValue(opcoes);
  api.tarefas.mockResolvedValue({ items: [], total: 0, page: 0, size: 25 });
  api.projetos.mockResolvedValue({
    items: [{ id: "projeto", titulo: "Encontro", status: "PLANEJADO" }],
    total: 1,
    page: 0,
    size: 24,
  });
  window.matchMedia = vi
    .fn()
    .mockReturnValue({
      matches: false,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    });
});
afterEach(cleanup);
it("envia projeto opcional da tarefa e avisa ao trocar equipe", async () => {
  const salvar: Mock = vi.fn().mockResolvedValue(undefined);
  render(
    <FormularioTarefa
      opcoes={opcoes}
      podeAtribuir={false}
      podeVerProjetos
      ocupada={false}
      aoSalvar={salvar}
    />,
  );
  fireEvent.change(screen.getByLabelText("Título"), {
    target: { value: "Pauta" },
  });
  fireEvent.change(screen.getByRole("combobox", { name: "Equipe" }), {
    target: { value: "equipe" },
  });
  await screen.findByRole("option", { name: "Encontro" });
  fireEvent.change(screen.getByRole("combobox", { name: "Projeto" }), {
    target: { value: "projeto" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Criar tarefa" }));
  await waitFor(() =>
    expect(salvar).toHaveBeenLastCalledWith(
      expect.objectContaining({ projetoId: "projeto" }),
    ),
  );
  fireEvent.change(screen.getByRole("combobox", { name: "Equipe" }), {
    target: { value: "outra" },
  });
  expect(
    screen.getByText("O projeto foi removido porque pertence a outra equipe."),
  ).toBeTruthy();
  await screen.findByRole("option", { name: "Encontro" });
  fireEvent.click(screen.getByRole("button", { name: "Criar tarefa" }));
  await waitFor(() =>
    expect(salvar).toHaveBeenLastCalledWith(
      expect.objectContaining({ projetoId: null }),
    ),
  );
});
it("interpreta o filtro do link do projeto sem segundo Kanban", async () => {
  render(
    <QuadroTarefas
      usuarioId="eu"
      permissoes={["tasks.view", "projects.view"]}
      opcoesIniciais={opcoes}
      projetoInicial="projeto"
    />,
  );
  await waitFor(() =>
    expect(api.tarefas).toHaveBeenCalledWith(
      expect.objectContaining({ projectId: "projeto", status: "A_FAZER" }),
    ),
  );
  expect(await screen.findByRole("combobox", { name: "Projeto" })).toBeTruthy();
});
it("Dashboard consulta e apresenta resumo real somente com projects.view", async () => {
  api.sessao.mockResolvedValue({
    id: "eu",
    name: "Ana",
    permissions: ["projects.view"],
    role: { name: "Suporte" },
  });
  api.ler.mockResolvedValue({
    projetosAtivos: 3,
    emAndamento: 2,
    comPrazoProximo: 1,
  });
  render(await PainelPage());
  expect(api.ler).toHaveBeenCalledWith("/projects/summary");
  expect(
    screen.getByRole("region", { name: "Resumo dos projetos" }),
  ).toBeTruthy();
  expect(screen.getByText("Projetos ativos")).toBeTruthy();
});
it("Dashboard não reserva espaço nem consulta projetos sem permissão", async () => {
  api.sessao.mockResolvedValue({
    id: "eu",
    name: "Ana",
    permissions: [],
    role: { name: "Suporte" },
  });
  render(await PainelPage());
  expect(api.ler).not.toHaveBeenCalledWith("/projects/summary");
  expect(
    screen.queryByRole("region", { name: "Resumo dos projetos" }),
  ).toBeNull();
});
