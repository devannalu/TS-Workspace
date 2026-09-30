import { describe, expect, it } from "vitest";
import { createInviteToken, getInviteState, hashInviteToken } from "../../src/lib/invites/token";

describe("convites", () => {
  it("gera token de alta entropia e guarda apenas hash", () => {
    const first = createInviteToken();
    const second = createInviteToken();
    expect(first.token).toMatch(/^[a-f0-9]{64}$/);
    expect(first.tokenHash).toBe(hashInviteToken(first.token));
    expect(first.tokenHash).not.toBe(first.token);
    expect(second.token).not.toBe(first.token);
  });
  it.each([
    [{ usedAt: null, cancelledAt: null, expiresAt: new Date(Date.now() + 1000) }, "pending"],
    [{ usedAt: new Date(), cancelledAt: null, expiresAt: new Date(0) }, "accepted"],
    [{ usedAt: null, cancelledAt: new Date(), expiresAt: new Date(Date.now() + 1000) }, "cancelled"],
    [{ usedAt: null, cancelledAt: null, expiresAt: new Date(0) }, "expired"],
  ] as const)("deriva estado %s", (invite, expected) => expect(getInviteState(invite)).toBe(expected));
});
