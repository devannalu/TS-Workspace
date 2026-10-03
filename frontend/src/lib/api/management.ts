import { getCsrf } from "./auth";

const javaApiUrl = process.env.NEXT_PUBLIC_JAVA_API_URL ?? "http://localhost:8080";

export async function managementRequest<T>(path: string, method = "GET", body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (method !== "GET") headers["X-XSRF-TOKEN"] = await getCsrf();
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(`${javaApiUrl}/api/v1${path}`, {
    method, headers, credentials: "include", cache: "no-store",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) throw new Error(`Não foi possível concluir a operação (${response.status}).`);
  return response.json() as Promise<T>;
}

export type JavaPage<T> = { items: T[]; total: number; page: number; size: number };
export type JavaRoleRef = { id: string; key: "SUPER_ADMIN" | "ADMIN" | "SUPERVISOR" | "SUPPORT"; name: string };
export type JavaTeamRef = { id: string; name: string };
