import { getCsrf } from "./auth";

const javaApiUrl = process.env.NEXT_PUBLIC_JAVA_API_URL ?? "http://localhost:8080";

export type JavaTeam = {
  id: string;
  key: string;
  name: string;
  description: string | null;
  parentId: string | null;
  archived: boolean;
  memberCount: number;
};
export type JavaTeamDetail = {
  team: JavaTeam;
  members: { id: string; name: string; email: string }[];
};
export type JavaTeamInput = { name: string; description?: string | null; parentId: string };

async function request<T>(path: string, method = "GET", body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (method !== "GET") headers["X-XSRF-TOKEN"] = await getCsrf();
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(`${javaApiUrl}/api/v1/teams${path}`, {
    method, headers, credentials: "include", cache: "no-store",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) throw new Error(`Não foi possível concluir a operação de equipes (${response.status}).`);
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const listJavaTeams = () => request<JavaTeam[]>("");
export const getJavaTeam = (id: string) => request<JavaTeamDetail>(`/${encodeURIComponent(id)}`);
export const createJavaTeam = (input: JavaTeamInput) => request<JavaTeamDetail>("", "POST", input);
export const editJavaTeam = (id: string, input: JavaTeamInput) => request<JavaTeamDetail>(`/${encodeURIComponent(id)}`, "PUT", input);
export const archiveJavaTeam = (id: string) => request<JavaTeamDetail>(`/${encodeURIComponent(id)}/archive`, "POST");
export const addJavaTeamMember = (id: string, userId: string) => request<void>(`/${encodeURIComponent(id)}/members`, "POST", { userId });
export const removeJavaTeamMember = (id: string, userId: string) => request<void>(`/${encodeURIComponent(id)}/members/${encodeURIComponent(userId)}`, "DELETE");
