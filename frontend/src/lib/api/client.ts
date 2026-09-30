const javaApiUrl = process.env.NEXT_PUBLIC_JAVA_API_URL ?? "http://localhost:8080";

export async function getJavaHealth(): Promise<{ status: "UP" }> {
  const response = await fetch(`${javaApiUrl}/api/v1/health`, {
    credentials: "include",
    cache: "no-store",
    signal: AbortSignal.timeout(10_000),
  });
  if (!response.ok) throw new Error("Serviço Java indisponível.");
  const body: unknown = await response.json();
  if (!body || typeof body !== "object" || !("status" in body) || body.status !== "UP") {
    throw new Error("Resposta inesperada do serviço Java.");
  }
  return { status: "UP" };
}
