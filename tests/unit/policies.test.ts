import { describe, expect, it } from "vitest";
import { resolvePermission, type PermissionContext } from "../../src/lib/permissions/policy";
import { hasActiveProfile } from "../../src/lib/auth/access-policy";
import { decideBootstrap } from "../../src/lib/auth/bootstrap-policy";
import { validateTeamParent } from "../../src/lib/teams/hierarchy";
import { requireTestDatabase } from "../../src/lib/db/test-safety";

const context: PermissionContext = { active: true, role: "SUPPORT", rolePermissions: ["teams.view"], overrides: {} };
describe("resolução RBAC", () => {
  it("nega por padrão", () => expect(resolvePermission(context, "users.manage")).toBe(false));
  it("herda permissões do cargo", () => expect(resolvePermission(context, "teams.view")).toBe(true));
  it("ALLOW concede permissão ausente do cargo", () => expect(resolvePermission({ ...context, overrides: { "users.view": "ALLOW" } }, "users.view")).toBe(true));
  it("DENY prevalece sobre o cargo", () => expect(resolvePermission({ ...context, overrides: { "teams.view": "DENY" } }, "teams.view")).toBe(false));
  it("SUPER_ADMIN tem bypass centralizado, inclusive de DENY", () => expect(resolvePermission({ ...context, role: "SUPER_ADMIN", overrides: { "teams.view": "DENY" } }, "teams.view")).toBe(true));
  it("inactive não recebe bypass", () => expect(resolvePermission({ ...context, active: false, role: "SUPER_ADMIN" }, "teams.view")).toBe(false));
});
describe("Profile", () => {
  it.each([null, undefined, { status: "inactive" }, { status: "invited" }])("bloqueia perfil ausente ou não ativo (%j)", profile => expect(hasActiveProfile(profile)).toBe(false));
  it("aceita perfil active", () => expect(hasActiveProfile({ status: "active" })).toBe(true));
});
const tree = [
  { id: "root", parentId: null, archivedAt: null },
  { id: "a", parentId: "root", archivedAt: null },
  { id: "b", parentId: "a", archivedAt: null },
  { id: "c", parentId: "b", archivedAt: null },
  { id: "archived", parentId: "root", archivedAt: new Date() },
];
describe("hierarquia", () => {
  it("permite subequipe válida", () => expect(() => validateTeamParent(tree, "new", "a")).not.toThrow());
  it("permite mover para outra equipe válida", () => expect(() => validateTeamParent(tree, "c", "root")).not.toThrow());
  it("impede self-parent", () => expect(() => validateTeamParent(tree, "a", "a")).toThrow());
  it("impede ciclo direto", () => expect(() => validateTeamParent(tree, "a", "b")).toThrow());
  it("impede ciclo indireto por descendente", () => expect(() => validateTeamParent(tree, "a", "c")).toThrow());
  it("impede parent inexistente", () => expect(() => validateTeamParent(tree, "a", "missing")).toThrow());
  it("impede parent arquivado", () => expect(() => validateTeamParent(tree, "a", "archived")).toThrow());
  it("impede segunda raiz", () => expect(() => validateTeamParent(tree, "new", null)).toThrow());
  it("preserva a raiz", () => expect(() => validateTeamParent(tree, "root", "a")).toThrow());
  it("detecta ciclo já existente", () => expect(() => validateTeamParent([{ id: "x", parentId: "y", archivedAt: null }, { id: "y", parentId: "x", archivedAt: null }], "new", "x")).toThrow());
});
describe("bootstrap", () => {
  it("autoriza a primeira criação explícita", () => expect(decideBootstrap({ existingUserId: null, superAdminIds: [], fullyProvisioned: false })).toBe("create"));
  it("é idempotente para a mesma conta completa", () => expect(decideBootstrap({ existingUserId: "a", superAdminIds: ["a"], fullyProvisioned: true })).toBe("already-provisioned"));
  it("recusa conta preexistente sem elevação", () => expect(() => decideBootstrap({ existingUserId: "a", superAdminIds: [], fullyProvisioned: false })).toThrow());
  it("recusa estado parcial", () => expect(() => decideBootstrap({ existingUserId: "a", superAdminIds: ["a"], fullyProvisioned: false })).toThrow());
  it("recusa segunda Super Admin", () => expect(() => decideBootstrap({ existingUserId: null, superAdminIds: ["a"], fullyProvisioned: false })).toThrow());
});
describe("isolamento de testes", () => {
  it("recusa URL de desenvolvimento", () => expect(() => requireTestDatabase("mysql://ts_workspace:x@127.0.0.1:3307/ts_workspace")).toThrow());
  it("recusa host externo", () => expect(() => requireTestDatabase("mysql://ts_workspace_test:x@example.com:3308/ts_workspace_test")).toThrow());
  it("recusa URL compartilhada", () => expect(() => requireTestDatabase("x", "x")).toThrow());
});
