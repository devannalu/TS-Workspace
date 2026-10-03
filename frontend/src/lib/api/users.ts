import { managementRequest, type JavaPage, type JavaRoleRef, type JavaTeamRef } from "./management";

export type JavaManagedUser = {
  id: string; name: string; email: string; jobTitle: string | null;
  status: "ACTIVE" | "INACTIVE"; role: JavaRoleRef; teams: JavaTeamRef[]; createdAt: string;
};
export type JavaUserFilters = { page?: number; size?: number; status?: "ACTIVE" | "INACTIVE"; roleId?: string; teamId?: string; search?: string };
export type JavaUserPatch = { jobTitle?: string; roleId?: string; teamIds?: string[] };

export function listJavaUsers(filters: JavaUserFilters = {}) {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) if (value !== undefined) query.set(key, String(value));
  return managementRequest<JavaPage<JavaManagedUser>>(`/users?${query}`);
}
export const getJavaUser = (id: string) => managementRequest<JavaManagedUser>(`/users/${encodeURIComponent(id)}`);
export const getJavaUserOptions = () => managementRequest<{ roles: JavaRoleRef[]; teams: JavaTeamRef[] }>("/users/options");
export const editJavaUser = (id: string, input: JavaUserPatch) => managementRequest<JavaManagedUser>(`/users/${encodeURIComponent(id)}`, "PATCH", input);
export const deactivateJavaUser = (id: string) => managementRequest<JavaManagedUser>(`/users/${encodeURIComponent(id)}/deactivate`, "POST");
export const activateJavaUser = (id: string) => managementRequest<JavaManagedUser>(`/users/${encodeURIComponent(id)}/activate`, "POST");
