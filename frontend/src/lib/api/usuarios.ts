import { requisitarJava } from "./http";
import type { PaginaApi, PerfilAcessoReferencia, EquipeReferencia } from "./contratos";
export type UsuarioGerenciado = {
  id: string;
  name: string;
  email: string;
  jobTitle: string | null;
  status: "ACTIVE" | "INACTIVE";
  role: PerfilAcessoReferencia;
  teams: EquipeReferencia[];
  createdAt: string;
};
export type FiltrosUsuarios = {
  page?: number;
  size?: number;
  status?: "ACTIVE" | "INACTIVE";
  roleId?: string;
  teamId?: string;
  search?: string;
};
export type AtualizarUsuarioRequest = {
  jobTitle?: string;
  roleId?: string;
  teamIds?: string[];
};
export function listarUsuarios(filters: FiltrosUsuarios = {}) {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(filters))
    if (value !== undefined)
      query.set(key, String(value));
  return requisitarJava<PaginaApi<UsuarioGerenciado>>(`/users?${query}`);
}
export const editarUsuario = (id: string, input: AtualizarUsuarioRequest) => requisitarJava<UsuarioGerenciado>(`/users/${encodeURIComponent(id)}`, "PATCH", input);
export const inativarUsuario = (id: string) => requisitarJava<UsuarioGerenciado>(`/users/${encodeURIComponent(id)}/deactivate`, "POST");
export const reativarUsuario = (id: string) => requisitarJava<UsuarioGerenciado>(`/users/${encodeURIComponent(id)}/activate`, "POST");
