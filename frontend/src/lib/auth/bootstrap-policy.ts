export type BootstrapState = {
  existingUserId: string | null;
  superAdminIds: readonly string[];
  fullyProvisioned: boolean;
};

export function decideBootstrap(state: BootstrapState): "create" | "already-provisioned" {
  if (state.superAdminIds.some(id => id !== state.existingUserId)) throw new Error("Uma Super Admin já existe; bootstrap não cria outra.");
  if (state.existingUserId) {
    if (state.fullyProvisioned && state.superAdminIds.length === 1) return "already-provisioned";
    throw new Error("Conta preexistente ou estado parcial; revisão administrativa necessária.");
  }
  if (state.superAdminIds.length > 0) throw new Error("Uma Super Admin já existe.");
  return "create";
}
