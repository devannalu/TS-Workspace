import { afterEach, beforeEach, it, expect, vi } from "vitest";
import { requisitarJava, limparCsrf, ErroApi, lerResposta } from "../../src/lib/api/http";
const fetchMock = vi.fn();
beforeEach(() => {
  limparCsrf();
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockReset();
});
afterEach(() => vi.unstubAllGlobals());
it("falha de rede tem mensagem controlada e não reenvia operação", async () => {
  fetchMock.mockRejectedValue(new TypeError("private-network-detail"));
  await expect(requisitarJava("/users")).rejects.toMatchObject({
    status: 0,
    message: "Não foi possível conectar. Tente novamente em instantes.",
  });
  expect(fetchMock).toHaveBeenCalledTimes(1);
});
it("falha na preparação CSRF permite nova ação explícita", async () => {
  fetchMock
    .mockRejectedValueOnce(new TypeError("private"))
    .mockResolvedValueOnce(new Response(JSON.stringify({ token: "test" })))
    .mockResolvedValueOnce(new Response(null, { status: 204 }));
  await expect(requisitarJava("/teams", "POST")).rejects.toBeInstanceOf(ErroApi);
  await requisitarJava("/teams", "POST");
  expect(fetchMock).toHaveBeenCalledTimes(3);
});
it("resposta inválida não revela conteúdo da exceção", async () => {
  fetchMock.mockResolvedValue(new Response("private-stack-html", { status: 200 }));
  await expect(requisitarJava("/users")).rejects.toMatchObject({
    status: 502,
    message: "Não foi possível ler a resposta. Tente novamente em instantes.",
  });
});

it.each([
  "Esta mensagem foi alterada. Atualize a conversa e tente novamente.",
  "Esta nota foi alterada. Atualize os dados e tente novamente.",
  "Esta parceria foi atualizada. Atualize os dados e tente novamente.",
])("conflito contextual conhecido permanece legível: %s",async detail=>{
  await expect(lerResposta(new Response(JSON.stringify({detail}),{status:409}))).rejects.toMatchObject({status:409,message:detail});
});
it("conflito técnico desconhecido não revela SQL",async()=>{
  await expect(lerResposta(new Response(JSON.stringify({detail:"private SQL stack"}),{status:409}))).rejects.toMatchObject({status:409,message:"A operação conflita com o estado atual do workspace."});
});
