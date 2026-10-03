import "server-only";
import { cache } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { urlApiJava, lerResposta } from "./api/http";
import type { UsuarioAtual } from "./api/autenticacao";
import { lerJava } from "./api/server";
import type { Equipe } from "./api/equipes";
export const buscarUsuarioDaSessao = cache(async () => {
  const cookieSessao = (await cookies()).get("TS_SESSION");
  if (!cookieSessao) return null;
  const response = await fetch(urlApiJava + "/api/v1/auth/me", {
    headers: { Cookie: "TS_SESSION=" + cookieSessao.value },
    cache: "no-store",
  });
  if (response.status === 401 || response.status === 403) return null;
  return lerResposta<UsuarioAtual>(response);
});
export async function exigirSessao() {
  const usuario = await buscarUsuarioDaSessao();
  if (!usuario) redirect("/login");
  return usuario;
}
export async function buscarMinhasEquipes(usuario: UsuarioAtual) {
  if (!usuario.permissions.includes("teams.view")) return [];
  return lerJava<Equipe[]>("/teams/mine");
}
