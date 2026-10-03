export const javaApiUrl =
  process.env.NEXT_PUBLIC_JAVA_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

export async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const messages: Record<number, string> = {
      400: "Confira os dados informados.",
      401: "Sessão encerrada ou credenciais inválidas. Entre novamente.",
      403: "Você não tem permissão para esta ação.",
      404: "Registro não encontrado.",
      409: "A operação conflita com o estado atual do workspace.",
    };
    // Detalhes técnicos do servidor não devem chegar à interface.
    await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      messages[response.status] ??
        "Serviço indisponível. Tente novamente em instantes.",
    );
  }
  return response.status === 204
    ? (undefined as T)
    : (response.json().catch(() => {
        throw new ApiError(
          502,
          "Não foi possível ler a resposta. Tente novamente em instantes.",
        );
      }) as Promise<T>);
}

let csrf: Promise<string> | undefined;
export function clearCsrf() {
  csrf = undefined;
}
export function getCsrf(): Promise<string> {
  return (csrf ??= fetch(`${javaApiUrl}/api/v1/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  })
    .then(readResponse<{ token: string }>)
    .then((body) => {
      if (!body.token) throw new Error("Não foi possível preparar a operação.");
      return body.token;
    })
    .catch((error) => {
      clearCsrf();
      throw error instanceof TypeError
        ? new ApiError(
            0,
            "Não foi possível conectar. Tente novamente em instantes.",
          )
        : error;
    }));
}

export async function javaRequest<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (method !== "GET") headers["X-XSRF-TOKEN"] = await getCsrf();
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(`${javaApiUrl}/api/v1${path}`, {
    method,
    headers,
    credentials: "include",
    cache: "no-store",
    body: body === undefined ? undefined : JSON.stringify(body),
  }).catch(() => {
    throw new ApiError(
      0,
      "Não foi possível conectar. Tente novamente em instantes.",
    );
  });
  // Renovamos o CSRF na próxima ação explícita, sem repetir mutações.
  if (response.status === 401 || response.status === 403) clearCsrf();
  if (
    response.status === 401 &&
    path !== "/auth/login" &&
    typeof window !== "undefined"
  )
    window.dispatchEvent(new Event("java-session-expired"));
  return readResponse<T>(response);
}
