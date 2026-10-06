import { beforeEach, afterEach, expect, it, vi } from "vitest";
import {
  criarTarefa,
  moverTarefa,
  parametrosTarefas,
  formatarPrazoTarefa,
  mensagemConflitoTarefa,
  type Tarefa,
} from "../../src/lib/api/tarefas";
import { limparCsrf } from "../../src/lib/api/http";
const requisicao = vi.fn();
beforeEach(() => {
  limparCsrf();
  requisicao.mockReset();
  vi.stubGlobal("fetch", requisicao);
});
afterEach(() => vi.unstubAllGlobals());
it("envia versão e CSRF, sem repetir uma movimentação conflitante", async () => {
  requisicao
    .mockResolvedValueOnce(
      new Response(JSON.stringify({ token: "fixture-csrf" })),
    )
    .mockResolvedValueOnce(new Response("{}", { status: 409 }));
  await expect(
    moverTarefa({ id: "fixture", versao: 7 } as Tarefa, "CONCLUIDA"),
  ).rejects.toThrow(mensagemConflitoTarefa);
  expect(requisicao).toHaveBeenCalledTimes(2);
  expect(JSON.parse(requisicao.mock.calls[1][1].body)).toEqual({
    status: "CONCLUIDA",
    versao: 7,
  });
});
it("mantém erros 400 e 403 centralizados", async () => {
  for (const status of [400, 403]) {
    limparCsrf();
    requisicao
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: "fixture-csrf" })),
      )
      .mockResolvedValueOnce(new Response("{}", { status }));
    await expect(
      criarTarefa({
        titulo: "Fixture",
        descricao: null,
        equipeId: "fixture",
        prazo: null,
        prioridade: "MEDIA",
      }),
    ).rejects.toMatchObject({ status });
  }
});
it("codifica busca e datas sem converter o prazo por fuso", () => {
  expect(
    new URLSearchParams(
      parametrosTarefas({ search: "a & b", archived: false }, 2),
    ).get("search"),
  ).toBe("a & b");
  expect(formatarPrazoTarefa("2026-10-06")).toBe("06/10/2026");
});
