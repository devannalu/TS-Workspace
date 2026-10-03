import { ErroApi, limparCsrf, requisitarJava } from "./http";
export { obterCsrf } from "./http";
export type UsuarioAtual = {
  id: string;
  name: string;
  email: string;
  jobTitle: string | null;
  status: "ACTIVE" | "INACTIVE";
  role: {
    id: string;
    key: "SUPER_ADMIN" | "ADMIN" | "SUPERVISOR" | "SUPPORT";
    name: string;
  };
  permissions: string[];
};
export async function entrarNoWorkspace(email: string, password: string) {
  try {
    return await requisitarJava<UsuarioAtual>("/auth/login", "POST", { email, password });
  }
  finally {
    limparCsrf();
  }
}
export async function sairDoWorkspace() {
  try {
    await requisitarJava<void>("/auth/logout", "POST");
  }
  finally {
    limparCsrf();
  }
}
export async function buscarUsuarioAtual(): Promise<UsuarioAtual | null> {
  try {
    return await requisitarJava<UsuarioAtual>("/auth/me");
  }
  catch (error) {
    if (error instanceof ErroApi && error.status === 401)
      return null;
    throw error;
  }
}
