// @vitest-environment jsdom
import { beforeEach, afterEach, it, expect, vi } from "vitest";
import {
  render,
  screen,
  cleanup,
  fireEvent,
  waitFor,
} from "@testing-library/react";
import { Comentarios } from "../../src/components/comentarios/comentarios";
import { AbasColaboracao } from "../../src/components/comentarios/abas-colaboracao";
import { DetalheTarefa } from "../../src/components/tarefas/detalhe-tarefa";
import { DetalheProjeto } from "../../src/components/projetos/detalhe-projeto";
import { ErroApi } from "../../src/lib/api/http";
import {
  conflitoComentario,
  type Comentario,
} from "../../src/lib/api/comentarios";
const api = vi.hoisted(() => ({
  listar: vi.fn(),
  criar: vi.fn(),
  editar: vi.fn(),
  remover: vi.fn(),
  atividade: vi.fn(),
}));
vi.mock("../../src/lib/api/comentarios", async (original) => ({
  ...(await original<object>()),
  listarComentarios: api.listar,
  criarComentario: api.criar,
  editarComentario: api.editar,
  removerComentario: api.remover,
  listarAtividade: api.atividade,
}));
const comentario: Comentario = {
  id: "comentario",
  autora: { id: "ana", nome: "Ana Luiza" },
  conteudo: "Texto original",
  editado: false,
  removido: false,
  versao: 0,
  criadaEm: "2026-10-06T13:00:00Z",
  atualizadaEm: "2026-10-06T13:00:00Z",
  capacidades: { editar: true, remover: true },
};
const pagina = {
  items: [comentario],
  total: 1,
  page: 0,
  size: 25,
  podeComentar: true,
};
beforeEach(() => {
  vi.resetAllMocks();
  api.listar.mockResolvedValue(pagina);
  api.criar.mockResolvedValue({ ...comentario, id: "novo" });
  api.editar.mockResolvedValue({
    ...comentario,
    conteudo: "Atualizado",
    versao: 1,
    editado: true,
  });
  api.remover.mockResolvedValue({
    ...comentario,
    conteudo: null,
    removido: true,
    versao: 1,
    capacidades: { editar: false, remover: false },
  });
  api.atividade.mockResolvedValue({ items: [], total: 0, page: 0, size: 25 });
  HTMLDialogElement.prototype.showModal = function () {
    this.setAttribute("open", "");
  };
  HTMLDialogElement.prototype.close = function () {
    this.removeAttribute("open");
  };
});
afterEach(cleanup);
function conversa(recurso: "tasks" | "projects" = "tasks") {
  render(<Comentarios recurso={recurso} recursoId="recurso" />);
}
it("carrega somente ao abrir aba e navega pelo teclado", async () => {
  render(
    <AbasColaboracao recurso="tasks" recursoId="recurso">
      <p>Informações existentes</p>
    </AbasColaboracao>,
  );
  expect(api.listar).not.toHaveBeenCalled();
  const tab = screen.getByRole("tab", { name: "Detalhes" });
  tab.focus();
  fireEvent.keyDown(tab, { key: "ArrowRight" });
  expect(document.activeElement).toBe(
    screen.getByRole("tab", { name: "Comentários" }),
  );
  expect(await screen.findByText("Texto original")).toBeTruthy();
  fireEvent.keyDown(document.activeElement!, { key: "End" });
  expect(
    await screen.findByText("Ainda não há atividades registradas."),
  ).toBeTruthy();
});
it("cria comentário sem limpar antes de confirmação", async () => {
  conversa();
  const campo = await screen.findByLabelText("Escreva um comentário…");
  fireEvent.change(campo, { target: { value: "Texto novo\nsegunda linha" } });
  fireEvent.click(screen.getByRole("button", { name: "Comentar" }));
  await waitFor(() =>
    expect(api.criar).toHaveBeenCalledWith(
      "tasks",
      "recurso",
      "Texto novo\nsegunda linha",
    ),
  );
  await waitFor(() => expect((campo as HTMLTextAreaElement).value).toBe(""));
  expect(screen.getByText("Comentário adicionado.")).toBeTruthy();
});
it("edita inline e mostra indicador", async () => {
  conversa();
  fireEvent.click(
    await screen.findByRole("button", { name: "Editar comentário" }),
  );
  fireEvent.change(screen.getByLabelText("Editar comentário"), {
    target: { value: "Atualizado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Salvar comentário" }));
  expect(await screen.findByText("Atualizado")).toBeTruthy();
  expect(screen.getByText("Editado")).toBeTruthy();
});
it("remove somente após confirmação preservando placeholder", async () => {
  conversa();
  fireEvent.click(
    await screen.findByRole("button", { name: "Remover comentário" }),
  );
  expect(api.remover).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar remoção" }));
  expect(
    await screen.findByText("Comentário removido.", { selector: "article p" }),
  ).toBeTruthy();
  expect(screen.queryByText("Texto original")).toBeNull();
});
it("moderação não oferece edição alheia", async () => {
  api.listar.mockResolvedValue({
    ...pagina,
    items: [{ ...comentario, capacidades: { editar: false, remover: true } }],
    podeComentar: false,
  });
  conversa();
  expect(
    await screen.findByRole("button", { name: "Remover comentário" }),
  ).toBeTruthy();
  expect(
    screen.queryByRole("button", { name: "Editar comentário" }),
  ).toBeNull();
  expect(screen.queryByLabelText("Escreva um comentário…")).toBeNull();
});
it("SUPPORT consulta Projeto sem composer", async () => {
  api.listar.mockResolvedValue({
    ...pagina,
    podeComentar: false,
    items: [{ ...comentario, capacidades: { editar: false, remover: false } }],
  });
  conversa("projects");
  expect(await screen.findByText("Texto original")).toBeTruthy();
  expect(screen.queryByRole("button", { name: "Comentar" })).toBeNull();
  expect(
    screen.queryByRole("button", { name: "Remover comentário" }),
  ).toBeNull();
});
it("mantém rascunho de envio offline", async () => {
  api.criar.mockRejectedValue(new ErroApi(0, "Não foi possível conectar."));
  conversa();
  const campo = await screen.findByLabelText("Escreva um comentário…");
  fireEvent.change(campo, { target: { value: "Rascunho importante" } });
  fireEvent.click(screen.getByRole("button", { name: "Comentar" }));
  expect(await screen.findByRole("alert")).toHaveProperty(
    "textContent",
    expect.stringContaining("Não foi possível conectar."),
  );
  expect((campo as HTMLTextAreaElement).value).toBe("Rascunho importante");
});
it("conflito preserva edição e exige refresh explícito", async () => {
  api.editar.mockRejectedValueOnce(new ErroApi(409, conflitoComentario));
  conversa();
  fireEvent.click(
    await screen.findByRole("button", { name: "Editar comentário" }),
  );
  const campo = screen.getByLabelText(
    "Editar comentário",
  ) as HTMLTextAreaElement;
  fireEvent.change(campo, { target: { value: "Meu rascunho" } });
  fireEvent.click(screen.getByRole("button", { name: "Salvar comentário" }));
  expect(await screen.findByText(conflitoComentario)).toBeTruthy();
  expect(campo.value).toBe("Meu rascunho");
  expect(campo.disabled).toBe(true);
  api.listar.mockResolvedValue({
    ...pagina,
    items: [{ ...comentario, versao: 1 }],
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Atualizar comentários" }),
  );
  await waitFor(() => expect(campo.disabled).toBe(false));
  expect(campo.value).toBe("Meu rascunho");
  fireEvent.click(screen.getByRole("button", { name: "Salvar comentário" }));
  await waitFor(() =>
    expect(api.editar).toHaveBeenLastCalledWith(
      expect.objectContaining({ versao: 1 }),
      "Meu rascunho",
    ),
  );
});
it("não interpreta HTML nem Markdown", async () => {
  api.listar.mockResolvedValue({
    ...pagina,
    items: [
      {
        ...comentario,
        conteudo: "<script>window.invasao=true</script> **texto**",
      },
    ],
  });
  conversa();
  expect(
    await screen.findByText("<script>window.invasao=true</script> **texto**"),
  ).toBeTruthy();
  expect(document.querySelector("article script")).toBeNull();
});
it("empty state e Projeto permitem iniciar conversa", async () => {
  api.listar.mockResolvedValue({ ...pagina, items: [], total: 0 });
  conversa("projects");
  expect(await screen.findByText("Nenhum comentário ainda.")).toBeTruthy();
  expect(
    screen.getByText("Comece a conversa sobre este projeto."),
  ).toBeTruthy();
});
it("carrega anteriores em ordem cronológica", async () => {
  api.listar
    .mockResolvedValueOnce({ ...pagina, total: 26 })
    .mockResolvedValueOnce({
      ...pagina,
      items: [
        {
          ...comentario,
          id: "antigo",
          conteudo: "Antigo",
          criadaEm: "2026-10-05T13:00:00Z",
        },
      ],
      total: 26,
      page: 1,
    });
  conversa();
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Carregar comentários anteriores",
    }),
  );
  expect(await screen.findByText("Antigo")).toBeTruthy();
  expect(screen.getAllByRole("article")[0].textContent).toContain("Antigo");
});
it("loading e erro com retry", async () => {
  let rejeitar!: (erro: Error) => void;
  api.listar.mockReturnValueOnce(
    new Promise((_, reject) => {
      rejeitar = reject;
    }),
  );
  conversa();
  expect(screen.getByLabelText("Carregando")).toBeTruthy();
  rejeitar(new Error("Backend offline"));
  expect(await screen.findByRole("alert")).toBeTruthy();
  fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
  expect(await screen.findByText("Texto original")).toBeTruthy();
});
it("timeline segura e paginada", async () => {
  api.atividade.mockResolvedValue({
    items: [
      {
        id: "evento",
        tipo: "comment.removed",
        ator: { id: "ana", nome: "Ana" },
        criadaEm: comentario.criadaEm,
      },
    ],
    total: 26,
    page: 0,
    size: 25,
  });
  render(
    <AbasColaboracao recurso="projects" recursoId="p">
      <p>Detalhes</p>
    </AbasColaboracao>,
  );
  fireEvent.click(screen.getByRole("tab", { name: "Atividade" }));
  expect(
    await screen.findByText("removeu um comentário.", { exact: false }),
  ).toBeTruthy();
  fireEvent.click(
    screen.getByRole("button", { name: "Atividades mais antigas" }),
  );
  await waitFor(() =>
    expect(api.atividade).toHaveBeenCalledWith("projects", "p", 1),
  );
});
it("detalhes da Tarefa preservados e conversa contextual", async () => {
  const tarefa = {
    id: "t",
    titulo: "Pauta",
    descricao: "Descrição",
    status: "A_FAZER" as const,
    prioridade: "MEDIA" as const,
    prazo: null,
    ordem: 0,
    versao: 0,
    equipe: { id: "e", nome: "Equipe" },
    criadaPor: { id: "ana", nome: "Ana" },
    responsaveis: [],
    criadaEm: comentario.criadaEm,
    atualizadaEm: comentario.criadaEm,
    arquivada: false,
    atrasada: false,
    capacidades: { editar: true, atribuir: true, arquivar: true },
  };
  render(
    <DetalheTarefa
      inicial={tarefa}
      opcoes={{ equipes: [], responsaveis: [] }}
      podeAtribuir
      aoFechar={() => {}}
      aoConcluir={() => {}}
    />,
  );
  expect(screen.getByText("Pauta")).toBeTruthy();
  fireEvent.click(screen.getByRole("tab", { name: "Comentários" }));
  expect(await screen.findByText("Texto original")).toBeTruthy();
  expect(api.listar).toHaveBeenCalledWith("tasks", "t");
});
it("Projeto arquivado mantém abas e leitura", async () => {
  api.listar.mockResolvedValue({
    ...pagina,
    podeComentar: false,
    items: [{ ...comentario, capacidades: { editar: false, remover: false } }],
  });
  const projeto = {
    id: "p",
    titulo: "Projeto",
    descricao: null,
    status: "CONCLUIDO" as const,
    equipe: { id: "e", nome: "Equipe" },
    criadaPor: { id: "ana", nome: "Ana" },
    responsaveis: [],
    dataInicio: null,
    dataFim: null,
    versao: 0,
    arquivado: true,
    criadaEm: comentario.criadaEm,
    atualizadaEm: comentario.criadaEm,
    totalTarefas: 0,
    tarefasConcluidas: 0,
    percentualProgresso: null,
    emAndamento: 0,
    emRevisao: 0,
    aFazer: 0,
    capacidades: {
      editar: false,
      gerenciarResponsaveis: false,
      arquivar: false,
      trocarEquipe: false,
    },
  };
  render(
    <DetalheProjeto
      inicial={projeto}
      opcoes={{ equipes: [], responsaveis: [] }}
      podeGerenciar={false}
      podeVerTarefas
      aoFechar={() => {}}
      aoConcluir={() => {}}
    />,
  );
  fireEvent.click(screen.getByRole("tab", { name: "Comentários" }));
  expect(await screen.findByText("Texto original")).toBeTruthy();
  expect(screen.queryByRole("button", { name: "Comentar" })).toBeNull();
  fireEvent.click(screen.getByRole("tab", { name: "Atividade" }));
  expect(
    await screen.findByText("Ainda não há atividades registradas."),
  ).toBeTruthy();
});
