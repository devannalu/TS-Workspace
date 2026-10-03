import "server-only";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { javaApiUrl, readResponse } from "./http";

// Reads only. Mutations go browser -> Java, where cookies/CSRF are authoritative.
export async function javaRead<T>(path: string): Promise<T> {
  const session = (await cookies()).get("TS_SESSION");
  const response = await fetch(`${javaApiUrl}/api/v1${path}`, {
    headers: session ? { Cookie: `TS_SESSION=${session.value}`, Accept: "application/json" } : { Accept: "application/json" },
    cache: "no-store",
  });
  if (response.status === 401) redirect("/login");
  return readResponse<T>(response);
}
