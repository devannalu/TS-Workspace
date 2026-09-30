export function requireTestDatabase(value: string | undefined, developmentUrl?: string) {
  if (!value || value === developmentUrl) throw new Error("Banco de testes separado obrigatório.");
  let url: URL;
  try { url = new URL(value); } catch { throw new Error("Configuração de banco de testes inválida."); }
  if (url.protocol !== "mysql:" || url.hostname !== "127.0.0.1" || url.port !== "3308" || url.pathname !== "/ts_workspace_test" || url.username !== "ts_workspace_test") {
    throw new Error("Testes permitidos somente no MySQL local exclusivo ts_workspace_test, porta 3308.");
  }
  return value;
}
