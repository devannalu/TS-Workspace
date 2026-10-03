import "server-only";
import { cache } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { javaApiUrl, readResponse } from "../api/http";
import type { JavaUser } from "../api/auth";
import { javaRead } from "../api/server";
import type { JavaTeam } from "../api/teams";
export const getCurrentUser = cache(async () => {
  const session = (await cookies()).get("TS_SESSION");
  if (!session) return null;
  const response = await fetch(javaApiUrl + "/api/v1/auth/me", {
    headers: { Cookie: "TS_SESSION=" + session.value },
    cache: "no-store",
  });
  if (response.status === 401 || response.status === 403) return null;
  return readResponse<JavaUser>(response);
});
export async function requireAuth() {
  const user = await getCurrentUser();
  if (!user) redirect("/login");
  return user;
}
export async function getMyTeams(user: JavaUser) {
  if (!user.permissions.includes("teams.view")) return [];
  const teams = await javaRead<JavaTeam[]>("/teams/mine");
  return teams;
}
