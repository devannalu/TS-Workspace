import "server-only";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { urlApiJava, lerResposta } from "./http";
// Mutações seguem do navegador ao Java, que valida cookies e CSRF.
export async function lerJava<T>(path: string): Promise<T> {
  const session = (await cookies()).get("TS_SESSION");
  const response = await fetch(`${urlApiJava}/api/v1${path}`, {
    headers: session ? { Cookie: `TS_SESSION=${session.value}`, Accept: "application/json" } : { Accept: "application/json" },
    cache: "no-store",
  });
  if (response.status === 401)
    redirect("/login");
  return lerResposta<T>(response);
}
