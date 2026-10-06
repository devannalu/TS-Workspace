// @vitest-environment jsdom
import { beforeEach, afterEach, it, expect, vi } from "vitest";
import {
  render,
  screen,
  cleanup,
  fireEvent,
  waitFor,
} from "@testing-library/react";
import { ListaProjetos } from "../../src/components/projetos/lista-projetos";
import { DetalheProjeto } from "../../src/components/projetos/detalhe-projeto";
import { FormularioProjeto } from "../../src/components/projetos/formulario-projeto";
import { CardProjeto } from "../../src/components/projetos/card-projeto";
import { SeletorProjeto } from "../../src/components/projetos/seletor-projeto";
import {
  mensagemConflitoProjeto,
  type Projeto,
} from "../../src/lib/api/projetos";
import { ErroApi } from "../../src/lib/api/http";
import { navegacaoPermitida } from "../../src/lib/ui/permissoes";
const api = vi.hoisted(() => ({
  listar: vi.fn(),
  buscar: vi.fn(),
  opcoes: vi.fn(),
  criar: vi.fn(),
  editar: vi.fn(),
  arquivar: vi.fn(),
}));
vi.mock("../../src/lib/api/projetos", async (original) => ({
  ...(await original<object>()),
  listarProjetos: api.listar,
  buscarProjeto: api.buscar,
  buscarOpcoesProjetos: api.opcoes,
  criarProjeto: api.criar,
  editarProjeto: api.editar,
  arquivarProjeto: api.arquivar,
}));
const projeto: Projeto = {
  id: "projeto",
  titulo: "Encontro da equipe",
  descricao: "Preparar juntas",
  status: "PLANEJADO",
  equipe: { id: "equipe", nome: "Comunicação" },
  responsaveis: [{ id: "eu", nome: "Ana" }],
  criadaPor: { id: "eu", nome: "Ana" },
  dataInicio: null,
  dataFim: null,
  versao: 2,
  arquivado: false,
  criadaEm: "2026-10-06T10:00:00Z",
  atualizadaEm: "2026-10-06T10:00:00Z",
  totalTarefas: 4,
  tarefasConcluidas: 3,
  percentualProgresso: 75,
  emAndamento: 1,
  emRevisao: 0,
  aFazer: 0,
  capacidades: {
    editar: true,
    gerenciarResponsaveis: true,
    arquivar: true,
    trocarEquipe: false,
  },
};
const opcoes = {
  equipes: [projeto.equipe, { id: "outra", nome: "Eventos" }],
  responsaveis: projeto.responsaveis,
};
beforeEach(() => {
  vi.resetAllMocks();
  api.listar.mockResolvedValue({
    items: [projeto],
    total: 1,
    page: 0,
    size: 24,
  });
  api.buscar.mockResolvedValue(projeto);
  api.opcoes.mockResolvedValue(opcoes);
  api.criar.mockResolvedValue(projeto);
  api.editar.mockResolvedValue(projeto);
  HTMLDialogElement.prototype.showModal = function () {
    this.setAttribute("open", "");
  };
  HTMLDialogElement.prototype.close = function () {
    this.removeAttribute("open");
  };
});
afterEach(cleanup);
function detalhe(p: Projeto | null = projeto) {
  return render(
    <DetalheProjeto
      inicial={p}
      opcoes={opcoes}
      podeGerenciar
      podeVerTarefas
      aoFechar={vi.fn()}
      aoConcluir={vi.fn()}
    />,
  );
}
it("mostra Projetos na navegação somente com a permissão", () => {
  expect(navegacaoPermitida([]).some((p) => p.href === "/projetos")).toBe(
    false,
  );
  expect(
    navegacaoPermitida(["projects.view"]).some((p) => p.href === "/projetos"),
  ).toBe(true);
});
it("mostra progresso com texto e sem barra enganosa para projeto vazio", () => {
  const { rerender } = render(
    <CardProjeto projeto={projeto} aoAbrir={vi.fn()} />,
  );
  expect(screen.getByRole("progressbar").getAttribute("aria-valuenow")).toBe(
    "75",
  );
  expect(screen.getByText("3 de 4 tarefas concluídas")).toBeTruthy();
  rerender(
    <CardProjeto
      projeto={{
        ...projeto,
        totalTarefas: 0,
        tarefasConcluidas: 0,
        percentualProgresso: null,
      }}
      aoAbrir={vi.fn()}
    />,
  );
  expect(screen.queryByRole("progressbar")).toBeNull();
  expect(screen.getByText("Sem tarefas")).toBeTruthy();
});
it("abre detalhe com resumo e link para o mesmo Kanban", () => {
  detalhe();
  expect(
    screen.getByRole("link", { name: "Ver tarefas" }).getAttribute("href"),
  ).toBe("/tarefas?projetoId=projeto");
  expect(screen.getByText(/1 em andamento/)).toBeTruthy();
});
it("mantém SUPPORT somente leitura sem controles de mutação", async () => {
  const leitura = {
    ...projeto,
    capacidades: {
      editar: false,
      gerenciarResponsaveis: false,
      arquivar: false,
      trocarEquipe: false,
    },
  };
  detalhe(leitura);
  expect(screen.queryByRole("button", { name: "Editar projeto" })).toBeNull();
  expect(screen.queryByRole("button", { name: "Arquivar projeto" })).toBeNull();
  cleanup();
  render(
    <ListaProjetos
      usuarioId="eu"
      permissoes={["projects.view"]}
      opcoes={opcoes}
    />,
  );
  expect(screen.queryByRole("button", { name: "Novo projeto" })).toBeNull();
  expect(
    await screen.findByRole("button", {
      name: "Abrir projeto Encontro da equipe",
    }),
  ).toBeTruthy();
});
it("cria com período e responsáveis sem campos especulativos", async () => {
  const salvar = vi.fn().mockResolvedValue(undefined);
  render(
    <FormularioProjeto
      opcoes={opcoes}
      podeGerenciar
      ocupada={false}
      bloqueada={false}
      aoSalvar={salvar}
    />,
  );
  fireEvent.change(screen.getByLabelText("Título"), {
    target: { value: "Objetivo" },
  });
  fireEvent.change(screen.getByRole("combobox", { name: "Equipe" }), {
    target: { value: "equipe" },
  });
  await screen.findByLabelText("Ana");
  fireEvent.click(screen.getByLabelText("Ana"));
  fireEvent.change(screen.getByLabelText("Data final"), {
    target: { value: "2026-10-12" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Criar projeto" }));
  await waitFor(() =>
    expect(salvar).toHaveBeenCalledWith(
      expect.objectContaining({
        titulo: "Objetivo",
        responsavelIds: ["eu"],
        dataFim: "2026-10-12",
      }),
    ),
  );
});
it("preserva equipe histórica e bloqueia período invertido", async () => {
  const salvar = vi.fn();
  render(
    <FormularioProjeto
      projeto={projeto}
      opcoes={opcoes}
      podeGerenciar
      ocupada={false}
      bloqueada={false}
      aoSalvar={salvar}
    />,
  );
  expect(
    (screen.getByRole("combobox", { name: "Equipe" }) as HTMLSelectElement)
      .disabled,
  ).toBe(true);
  fireEvent.change(screen.getByLabelText("Data de início"), {
    target: { value: "2026-10-12" },
  });
  fireEvent.change(screen.getByLabelText("Data final"), {
    target: { value: "2026-10-01" },
  });
  await waitFor(() =>
    expect(
      (
        screen.getByRole("button", {
          name: "Salvar alterações",
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(false),
  );
  fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));
  expect(salvar).not.toHaveBeenCalled();
  expect(await screen.findByText(/A data final deve/)).toBeTruthy();
});
it("edita status e envia a versão recebida", async () => {
  detalhe();
  fireEvent.click(screen.getByRole("button", { name: "Editar projeto" }));
  fireEvent.change(screen.getByRole("combobox", { name: "Status" }), {
    target: { value: "PAUSADO" },
  });
  await waitFor(() =>
    expect(
      (
        screen.getByRole("button", {
          name: "Salvar alterações",
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(false),
  );
  fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));
  await waitFor(() =>
    expect(api.editar).toHaveBeenCalledWith(
      projeto,
      expect.objectContaining({ status: "PAUSADO" }),
    ),
  );
});
it("preserva rascunho no conflito de versão até atualização explícita", async () => {
  api.editar.mockRejectedValue(new ErroApi(409, mensagemConflitoProjeto));
  detalhe();
  fireEvent.click(screen.getByRole("button", { name: "Editar projeto" }));
  fireEvent.change(screen.getByLabelText("Título"), {
    target: { value: "Rascunho" },
  });
  await waitFor(() =>
    expect(
      (
        screen.getByRole("button", {
          name: "Salvar alterações",
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(false),
  );
  fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));
  await screen.findByText(mensagemConflitoProjeto);
  expect((screen.getByLabelText("Título") as HTMLInputElement).value).toBe(
    "Rascunho",
  );
  expect(
    (
      screen.getByRole("button", {
        name: "Salvar alterações",
      }) as HTMLButtonElement
    ).disabled,
  ).toBe(true);
  fireEvent.click(
    screen.getByRole("button", { name: "Atualizar dados do projeto" }),
  );
  await screen.findByRole("button", { name: "Editar projeto" });
  expect(api.buscar).toHaveBeenCalledTimes(1);
});
it("explica pendências de conclusão sem bloquear o formulário como conflito de versão", async () => {
  api.editar.mockRejectedValue(
    new ErroApi(
      409,
      "Conclua ou arquive as tarefas pendentes antes de concluir o projeto.",
    ),
  );
  detalhe();
  fireEvent.click(screen.getByRole("button", { name: "Editar projeto" }));
  await waitFor(() =>
    expect(
      (
        screen.getByRole("button", {
          name: "Salvar alterações",
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(false),
  );
  fireEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));
  await screen.findByText(/pendentes antes de concluir/);
  expect(
    screen.queryByRole("button", { name: "Atualizar dados do projeto" }),
  ).toBeNull();
  expect(
    (
      screen.getByRole("button", {
        name: "Salvar alterações",
      }) as HTMLButtonElement
    ).disabled,
  ).toBe(false);
});
it("confirma arquivo com explicação e apresenta conflito contextual", async () => {
  api.arquivar.mockRejectedValue(
    new ErroApi(
      409,
      "Conclua ou arquive as tarefas pendentes antes de arquivar o projeto.",
    ),
  );
  detalhe();
  fireEvent.click(screen.getByRole("button", { name: "Arquivar projeto" }));
  expect(
    screen.getByText(/histórico e as tarefas concluídas serão preservados/),
  ).toBeTruthy();
  fireEvent.click(
    screen.getByRole("button", { name: "Confirmar arquivamento" }),
  );
  await screen.findByText(/pendentes antes de arquivar/);
  expect(api.arquivar).toHaveBeenCalledTimes(1);
});
it("filtra responsáveis e limpa os filtros sem carregar por card", async () => {
  render(
    <ListaProjetos
      usuarioId="eu"
      permissoes={["projects.view"]}
      opcoes={opcoes}
    />,
  );
  await screen.findByRole("button", {
    name: "Abrir projeto Encontro da equipe",
  });
  fireEvent.click(screen.getByRole("button", { name: "Sou responsável" }));
  expect(
    (screen.getByRole("combobox", { name: "Responsável" }) as HTMLSelectElement)
      .value,
  ).toBe("eu");
  await waitFor(() =>
    expect(api.listar).toHaveBeenLastCalledWith({ responsibleId: "eu" }, 0),
  );
  fireEvent.click(screen.getByRole("button", { name: "Limpar filtros" }));
  await waitFor(() => expect(api.listar).toHaveBeenLastCalledWith({}, 0));
  expect(api.buscar).not.toHaveBeenCalled();
});
it("apresenta erro de rede com recuperação explícita", async () => {
  api.listar.mockRejectedValue(new ErroApi(0, "Não foi possível conectar."));
  render(
    <ListaProjetos
      usuarioId="eu"
      permissoes={["projects.view"]}
      opcoes={opcoes}
    />,
  );
  await screen.findByRole("alert");
  expect(screen.getByRole("button", { name: "Tentar novamente" })).toBeTruthy();
});
it("seleciona somente projetos elegíveis da equipe e pagina opções", async () => {
  api.listar
    .mockResolvedValueOnce({
      items: [projeto, { ...projeto, id: "finalizado", status: "CONCLUIDO" }],
      total: 30,
      page: 0,
      size: 24,
    })
    .mockResolvedValueOnce({
      items: [{ ...projeto, id: "segundo", titulo: "Segundo" }],
      total: 30,
      page: 1,
      size: 24,
    });
  const selecionar = vi.fn();
  render(
    <SeletorProjeto
      equipeId="equipe"
      valor=""
      somenteElegiveis
      aoSelecionar={selecionar}
    />,
  );
  await screen.findByRole("option", { name: projeto.titulo });
  expect(screen.queryByRole("option", { name: "Concluído" })).toBeNull();
  expect(screen.getAllByRole("option")).toHaveLength(2);
  fireEvent.click(
    screen.getByRole("button", { name: "Carregar mais projetos" }),
  );
  await screen.findByRole("option", { name: "Segundo" });
  fireEvent.change(screen.getByRole("combobox", { name: "Projeto" }), {
    target: { value: "segundo" },
  });
  expect(selecionar).toHaveBeenCalledWith("segundo");
});
