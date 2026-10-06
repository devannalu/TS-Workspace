import { beforeEach, afterEach, it, expect, vi } from "vitest";
import {
  editarProjeto,
  parametrosProjetos,
  type Projeto,
} from "../../src/lib/api/projetos";
import { moverTarefa, type Tarefa } from "../../src/lib/api/tarefas";
import { limparCsrf, lerResposta } from "../../src/lib/api/http";
const fetchMock = vi.fn();
beforeEach(() => {
  limparCsrf();
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});
afterEach(() => vi.unstubAllGlobals());
it("envia versão e CSRF sem repetir edição rejeitada", async () => {
  fetchMock
    .mockResolvedValueOnce(new Response(JSON.stringify({ token: "fixture" })))
    .mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          detail:
            "Conclua ou arquive as tarefas pendentes antes de concluir o projeto.",
        }),
        { status: 409 },
      ),
    );
  await expect(
    editarProjeto({ id: "p", versao: 4 } as Projeto, {
      titulo: "Título",
      descricao: null,
      equipeId: "e",
      dataInicio: null,
      dataFim: null,
      status: "CONCLUIDO",
    }),
  ).rejects.toThrow("pendentes antes de concluir");
  expect(fetchMock).toHaveBeenCalledTimes(2);
  expect(JSON.parse(fetchMock.mock.calls[1][1].body).versao).toBe(4);
});
it("preserva conflito de projeto ao reabrir uma tarefa", async () => {
  fetchMock
    .mockResolvedValueOnce(new Response(JSON.stringify({ token: "fixture" })))
    .mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          detail: "Reabra o projeto antes de reabrir esta tarefa.",
        }),
        { status: 409 },
      ),
    );
  await expect(
    moverTarefa({ id: "t", versao: 2 } as Tarefa, "A_FAZER"),
  ).rejects.toThrow("Reabra o projeto");
});
it("não exibe detalhes técnicos desconhecidos", async () => {
  await expect(
    lerResposta(
      new Response(
        JSON.stringify({ detail: "SQL password_hash stack trace" }),
        { status: 409 },
      ),
    ),
  ).rejects.toThrow("A operação conflita com o estado atual do workspace.");
});
it("mantém filtros de período e busca corretamente codificados", () => {
  const p = new URLSearchParams(
    parametrosProjetos(
      {
        search: "a & b",
        startFrom: "2026-10-01",
        dueTo: "2026-10-30",
        responsibleId: "eu",
        archived: true,
      },
      2,
    ),
  );
  expect(p.get("search")).toBe("a & b");
  expect(p.get("page")).toBe("2");
  expect(p.get("dueTo")).toBe("2026-10-30");
});
