import { describe, it, expect, vi } from "vitest";
import { navigationFor, dashboardAccess } from "../../src/lib/ui/permissions";
import { dashboardTotals } from "../../src/lib/ui/dashboard";
import {
  friendlyDate,
  pageNumber,
  inviteLabels,
} from "../../src/lib/ui/format";
const user = {
  id: "fixture",
  name: "Exemplo",
  email: "fixture@example.test",
  jobTitle: null,
  status: "ACTIVE" as const,
  role: { id: "role", key: "SUPPORT" as const, name: "Suporte" },
  permissions: ["teams.view"],
};
describe("dashboard e permissions", () => {
  it("não consulta dados administrativos sem users.view", async () => {
    const read = vi.fn();
    expect(await dashboardTotals(user, read)).toEqual({
      activeUsers: null,
      pendingInvites: null,
    });
    expect(read).not.toHaveBeenCalled();
    expect(dashboardAccess(user)).toMatchObject({
      users: false,
      invite: false,
      teams: true,
    });
    expect(navigationFor(user.permissions).map((i) => i.href)).toEqual([
      "/workspace",
      "/equipes",
    ]);
  });
  it("consulta totais reais sem percorrer listas paginadas", async () => {
    const read = vi
      .fn()
      .mockImplementation((path: string) =>
        Promise.resolve(path.startsWith("/users") ? { total: 42 } : 2),
      );
    expect(
      await dashboardTotals({ ...user, permissions: ["users.view"] }, read),
    ).toEqual({ activeUsers: 42, pendingInvites: 2 });
    expect(read).toHaveBeenCalledWith("/users?status=ACTIVE&size=1");
    expect(read).toHaveBeenCalledWith("/invites/pending-count");
    expect(read).toHaveBeenCalledTimes(2);
  });
  it("formata datas e normaliza paginação", () => {
    expect(friendlyDate("2026-10-02T12:00:00Z")).toContain("2026");
    expect(pageNumber("-1")).toBe(0);
    expect(pageNumber("3")).toBe(3);
    expect(pageNumber("bad")).toBe(0);
    expect(inviteLabels.USED).toBe("Usado");
  });
});
