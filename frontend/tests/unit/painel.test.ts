import { describe, it, expect, vi } from "vitest";
import { navegacaoPermitida, acessosPainel } from "../../src/lib/ui/permissoes";
import { buscarTotaisPainel } from "../../src/lib/ui/painel";
import { formatarData, normalizarPagina, rotulosSituacaoConvite, } from "../../src/lib/ui/formatacao";
const usuario = {
  id: "fixture",
  name: "Exemplo",
  email: "fixture@example.test",
  jobTitle: null,
  status: "ACTIVE" as const,
  role: { id: "role", key: "SUPPORT" as const, name: "Suporte" },
  permissions: ["teams.view"],
};
describe("painel e permissões", () => {
  it("não consulta dados administrativos sem users.view", async () => {
    const read = vi.fn();
    expect(await buscarTotaisPainel(usuario, read)).toEqual({
      usuariosAtivos: null,
      convitesPendentes: null,
    });
    expect(read).not.toHaveBeenCalled();
    expect(acessosPainel(usuario)).toMatchObject({
      podeVerUsuarios: false,
      podeCriarConvite: false,
      podeVerEquipes: true,
    });
    expect(navegacaoPermitida(usuario.permissions).map((i) => i.href)).toEqual([
      "/workspace",
      "/equipes",
    ]);
  });
  it("consulta totais reais sem percorrer listas paginadas", async () => {
    const read = vi
      .fn()
      .mockImplementation((path: string) => Promise.resolve(path.startsWith("/users") ? { total: 42 } : 2));
    expect(await buscarTotaisPainel({ ...usuario, permissions: ["users.view"] }, read)).toEqual({ usuariosAtivos: 42, convitesPendentes: 2 });
    expect(read).toHaveBeenCalledWith("/users?status=ACTIVE&size=1");
    expect(read).toHaveBeenCalledWith("/invites/pending-count");
    expect(read).toHaveBeenCalledTimes(2);
  });
  it("formata datas e normaliza paginação", () => {
    expect(formatarData("2026-10-02T12:00:00Z")).toContain("2026");
    expect(normalizarPagina("-1")).toBe(0);
    expect(normalizarPagina("3")).toBe(3);
    expect(normalizarPagina("bad")).toBe(0);
    expect(rotulosSituacaoConvite.USED).toBe("Usado");
  });
});
