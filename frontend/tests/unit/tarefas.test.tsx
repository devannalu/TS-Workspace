// @vitest-environment jsdom
import { beforeEach, afterEach, it, expect, vi } from "vitest";
import {
  render,
  screen,
  cleanup,
  fireEvent,
  waitFor,
} from "@testing-library/react";
import { QuadroTarefas } from "../../src/components/tarefas/quadro-tarefas";
import { DetalheTarefa } from "../../src/components/tarefas/detalhe-tarefa";
import { FormularioTarefa } from "../../src/components/tarefas/formulario-tarefa";
import { ErroApi } from "../../src/lib/api/http";
import { mensagemConflitoTarefa, type Tarefa } from "../../src/lib/api/tarefas";
const api = vi.hoisted(() => ({
  listar: vi.fn(),
  buscar: vi.fn(),
  opcoes: vi.fn(),
  criar: vi.fn(),
  editar: vi.fn(),
  mover: vi.fn(),
  arquivar: vi.fn(),
}));
vi.mock("../../src/lib/api/tarefas", async (original) => ({
  ...(await original<object>()),
  listarTarefas: api.listar,
  buscarTarefa: api.buscar,
  buscarOpcoesTarefas: api.opcoes,
  criarTarefa: api.criar,
  editarTarefa: api.editar,
  moverTarefa: api.mover,
  arquivarTarefa: api.arquivar,
}));
const tarefa: Tarefa = {
  id: "fixture",
  titulo: "Revisar pauta",
  descricao: "Descrição",
  status: "A_FAZER",
  prioridade: "ALTA",
  prazo: "2026-10-01",
  ordem: 0,
  versao: 4,
  equipe: { id: "equipe", nome: "Comunicação" },
  criadaPor: { id: "eu", nome: "Ana" },
  responsaveis: [{ id: "eu", nome: "Ana" }],
  criadaEm: "2026-10-01T10:00:00Z",
  atualizadaEm: "2026-10-01T10:00:00Z",
  arquivada: false,
  atrasada: true,
  capacidades: { editar: true, atribuir: true, arquivar: true },
};
const opcoes = { equipes: [tarefa.equipe], responsaveis: tarefa.responsaveis };
beforeEach(() => {
  vi.resetAllMocks();
  api.opcoes.mockResolvedValue(opcoes);
  api.buscar.mockResolvedValue(tarefa);
  api.mover.mockResolvedValue(tarefa);
  api.listar.mockImplementation(async (filtros) => ({
    items: filtros.status === "A_FAZER" ? [tarefa] : [],
    total: filtros.status === "A_FAZER" ? 1 : 0,
    page: 0,
    size: 25,
  }));
  window.matchMedia = vi.fn().mockReturnValue({
    matches: true,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  });
  HTMLDialogElement.prototype.showModal = function () {
    this.setAttribute("open", "");
  };
  HTMLDialogElement.prototype.close = function () {
    this.removeAttribute("open");
  };
});
afterEach(cleanup);
it("devolve foco ao controle depois de mover entre colunas", async () => {
  let atual = tarefa;
  api.listar.mockImplementation(async (filtros) => ({
    items: filtros.status === atual.status ? [atual] : [],
    total: filtros.status === atual.status ? 1 : 0,
    page: 0,
    size: 25,
  }));
  api.mover.mockImplementation(async (_, status) => {
    atual = { ...tarefa, status, versao: 5 };
    return atual;
  });
  quadro();
  const controle = await screen.findByLabelText("Mover Revisar pauta para");
  controle.focus();
  fireEvent.change(controle, { target: { value: "EM_ANDAMENTO" } });
  await waitFor(() =>
    expect(document.activeElement).toBe(
      screen.getByLabelText("Mover Revisar pauta para"),
    ),
  );
  await waitFor(() =>
    expect(
      (screen.getByLabelText("Mover Revisar pauta para") as HTMLSelectElement)
        .value,
    ).toBe("EM_ANDAMENTO"),
  );
});
it("carrega a próxima página da coluna sem consultar os cartões individualmente", async () => {
  api.listar.mockImplementation(async (filtros, pagina = 0) => ({
    items:
      filtros.status === "A_FAZER"
        ? [
            {
              ...tarefa,
              id: pagina ? "segunda" : tarefa.id,
              titulo: pagina ? "Segunda pauta" : tarefa.titulo,
            },
          ]
        : [],
    total: filtros.status === "A_FAZER" ? 2 : 0,
    page: pagina,
    size: 25,
  }));
  quadro();
  await screen.findByText("Revisar pauta");
  fireEvent.click(screen.getByRole("button", { name: "Carregar mais" }));
  await screen.findByText("Segunda pauta");
  expect(api.listar).toHaveBeenCalledWith({ status: "A_FAZER" }, 1);
  expect(api.buscar).not.toHaveBeenCalled();
});
function quadro() {
  return render(
    <QuadroTarefas
      usuarioId="eu"
      permissoes={["tasks.view", "tasks.create", "tasks.assign"]}
      opcoesIniciais={opcoes}
    />,
  );
}
it("carrega quatro colunas e move pelo controle acessível com a versão conhecida", async () => {
  quadro();
  await screen.findByRole("button", { name: "Abrir tarefa Revisar pauta" });
  expect(screen.getByText("Prioridade Alta")).toBeTruthy();
  expect(screen.getByText(/01\/10\/2026/)).toBeTruthy();
  fireEvent.change(screen.getByLabelText("Mover Revisar pauta para"), {
    target: { value: "EM_REVISAO" },
  });
  await waitFor(() =>
    expect(api.mover).toHaveBeenCalledWith(tarefa, "EM_REVISAO", undefined),
  );
});
it("filtra minhas tarefas e permite limpar sem criar outra página", async () => {
  quadro();
  await screen.findByText("Revisar pauta");
  fireEvent.click(screen.getByRole("button", { name: "Minhas tarefas" }));
  await waitFor(() =>
    expect(api.listar).toHaveBeenCalledWith(
      expect.objectContaining({ assigneeId: "eu" }),
    ),
  );
  fireEvent.click(screen.getByText("Limpar filtros"));
  await waitFor(() =>
    expect(
      screen
        .getByRole("button", { name: "Minhas tarefas" })
        .getAttribute("aria-pressed"),
    ).toBe("false"),
  );
});
it("não oferece edição ou arraste em tarefa somente para leitura", async () => {
  const leitura = {
    ...tarefa,
    capacidades: { editar: false, atribuir: false, arquivar: false },
  };
  api.listar.mockResolvedValue({
    items: [leitura],
    total: 1,
    page: 0,
    size: 25,
  });
  quadro();
  await screen.findAllByText("Revisar pauta");
  expect(screen.queryByLabelText("Mover Revisar pauta para")).toBeNull();
  expect(screen.queryByLabelText("Arrastar Revisar pauta")).toBeNull();
});
it("no celular usa segmentos e mantém o fallback sem exigir arraste", async () => {
  window.matchMedia = vi.fn().mockReturnValue({
    matches: false,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  });
  quadro();
  await screen.findByText("Revisar pauta");
  expect(screen.queryByLabelText("Arrastar Revisar pauta")).toBeNull();
  expect(screen.getByLabelText("Mover Revisar pauta para")).toBeTruthy();
  fireEvent.click(screen.getByRole("button", { name: "Em andamento" }));
  expect(
    screen
      .getByRole("button", { name: "Em andamento" })
      .getAttribute("aria-pressed"),
  ).toBe("true");
});
it("mostra erro de conexão e permite tentar novamente", async () => {
  api.listar.mockRejectedValueOnce(
    new ErroApi(0, "Não foi possível conectar."),
  );
  quadro();
  expect(await screen.findByRole("alert")).toBeTruthy();
  fireEvent.click(screen.getByText("Tentar novamente"));
  await screen.findByText("Revisar pauta");
});
it("mostra estado vazio sem inventar tarefas", async () => {
  api.listar.mockResolvedValue({ items: [], total: 0, page: 0, size: 25 });
  quadro();
  expect(await screen.findByText("Nenhuma tarefa encontrada")).toBeTruthy();
});
it("envia formulário sem prazo ou descrição e restringe responsáveis à equipe", async () => {
  const salvar = vi.fn().mockResolvedValue(undefined);
  render(
    <FormularioTarefa
      opcoes={opcoes}
      podeAtribuir
      ocupada={false}
      aoSalvar={salvar}
    />,
  );
  fireEvent.change(screen.getByLabelText("Título"), {
    target: { value: " Nova pauta " },
  });
  fireEvent.change(screen.getByLabelText("Equipe"), {
    target: { value: "equipe" },
  });
  await screen.findByLabelText("Ana");
  fireEvent.click(screen.getByLabelText("Ana"));
  fireEvent.click(screen.getByText("Criar tarefa"));
  await waitFor(() =>
    expect(salvar).toHaveBeenCalledWith({
      titulo: "Nova pauta",
      descricao: null,
      equipeId: "equipe",
      prazo: null,
      prioridade: "MEDIA",
      responsavelIds: ["eu"],
    }),
  );
});
it("em 409 preserva o formulário, não repete escrita e exige atualizar dados", async () => {
  api.editar.mockRejectedValue(new ErroApi(409, mensagemConflitoTarefa));
  render(
    <DetalheTarefa
      inicial={tarefa}
      opcoes={opcoes}
      podeAtribuir
      aoFechar={vi.fn()}
      aoConcluir={vi.fn()}
    />,
  );
  fireEvent.click(screen.getByText("Editar tarefa"));
  await screen.findByLabelText("Ana");
  fireEvent.click(screen.getByText("Salvar alterações"));
  expect(await screen.findByText(mensagemConflitoTarefa)).toBeTruthy();
  expect(api.editar).toHaveBeenCalledTimes(1);
  expect(screen.getByLabelText("Título")).toBeTruthy();
  fireEvent.click(screen.getByText("Atualizar dados"));
  await waitFor(() => expect(api.buscar).toHaveBeenCalledWith("fixture"));
});
it("pede confirmação para arquivar e usa a versão da tarefa", async () => {
  const concluir = vi.fn();
  api.arquivar.mockResolvedValue(tarefa);
  render(
    <DetalheTarefa
      inicial={tarefa}
      opcoes={opcoes}
      podeAtribuir
      aoFechar={vi.fn()}
      aoConcluir={concluir}
    />,
  );
  fireEvent.click(screen.getByRole("button", { name: "Arquivar tarefa" }));
  expect(api.arquivar).not.toHaveBeenCalled();
  fireEvent.click(screen.getByText("Confirmar arquivamento"));
  await waitFor(() => expect(api.arquivar).toHaveBeenCalledWith(tarefa));
});
