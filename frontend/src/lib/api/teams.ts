import { javaRequest } from "./http";


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

const request = <T>(path: string, method = "GET", body?: unknown) => javaRequest<T>("/teams" + path, method, body);

export const listJavaTeams = () => request<JavaTeam[]>("");
export const getJavaTeam = (id: string) => request<JavaTeamDetail>(`/${encodeURIComponent(id)}`);
export const createJavaTeam = (input: JavaTeamInput) => request<JavaTeamDetail>("", "POST", input);
export const editJavaTeam = (id: string, input: JavaTeamInput) => request<JavaTeamDetail>(`/${encodeURIComponent(id)}`, "PUT", input);
export const archiveJavaTeam = (id: string) => request<JavaTeamDetail>(`/${encodeURIComponent(id)}/archive`, "POST");
export const addJavaTeamMember = (id: string, userId: string) => request<void>(`/${encodeURIComponent(id)}/members`, "POST", { userId });
export const removeJavaTeamMember = (id: string, userId: string) => request<void>(`/${encodeURIComponent(id)}/members/${encodeURIComponent(userId)}`, "DELETE");
