import { requisitarJava } from "./http";
export type Equipe = {
  id: string;
  key: string;
  name: string;
  description: string | null;
  parentId: string | null;
  archived: boolean;
  memberCount: number;
};
export type DetalheEquipe = {
  team: Equipe;
  members: {
    id: string;
    name: string;
    email: string;
  }[];
};
export type SalvarEquipeRequest = {
  name: string;
  description?: string | null;
  parentId: string;
};
const request = <T>(path: string, method = "GET", body?: unknown) => requisitarJava<T>("/teams" + path, method, body);
export const buscarEquipe = (id: string) => request<DetalheEquipe>(`/${encodeURIComponent(id)}`);
export const criarEquipe = (input: SalvarEquipeRequest) => request<DetalheEquipe>("", "POST", input);
export const editarEquipe = (id: string, input: SalvarEquipeRequest) => request<DetalheEquipe>(`/${encodeURIComponent(id)}`, "PUT", input);
export const arquivarEquipe = (id: string) => request<DetalheEquipe>(`/${encodeURIComponent(id)}/archive`, "POST");
export const adicionarIntegranteEquipe = (id: string, userId: string) => request<void>(`/${encodeURIComponent(id)}/members`, "POST", { userId });
export const removerIntegranteEquipe = (id: string, userId: string) => request<void>(`/${encodeURIComponent(id)}/members/${encodeURIComponent(userId)}`, "DELETE");
