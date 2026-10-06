export const urlApiJava =
  process.env.NEXT_PUBLIC_JAVA_API_URL ?? "http://localhost:8080";
export class ErroApi extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
  }
}
export async function lerResposta<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const messages: Record<number, string> = {
      400: "Confira os dados informados.",
      401: "Sessão encerrada ou credenciais inválidas. Entre novamente.",
      403: "Você não tem permissão para esta ação.",
      404: "Registro não encontrado.",
      409: "A operação conflita com o estado atual do workspace.",
    };
    // Detalhes técnicos do servidor não devem chegar à interface.
    const problema = await response.json().catch(() => null);
    const conflitosConhecidos = [
      "Este comentário foi alterado. Atualize os dados e tente novamente.",
      "Este projeto foi atualizado por outra pessoa. Atualize os dados e tente novamente.",
      "Esta tarefa foi atualizada por outra pessoa. Atualize os dados e tente novamente.",
      "Conclua ou arquive as tarefas pendentes antes de concluir o projeto.",
      "Conclua ou arquive as tarefas pendentes antes de arquivar o projeto.",
      "A equipe não pode ser alterada porque este projeto já teve tarefas vinculadas.",
      "A tarefa e o projeto devem pertencer à mesma equipe.",
      "Reabra o projeto antes de vincular novas tarefas.",
      "Reabra o projeto antes de reabrir esta tarefa.",
      "Projeto arquivado: somente leitura.",
      "Equipe arquivada: projetos históricos são somente leitura.",
      "Equipe arquivada: tarefas históricas são somente leitura.",
    ];
    if (
      response.status === 409 &&
      conflitosConhecidos.includes(problema?.detail)
    )
      throw new ErroApi(409, problema.detail);
    throw new ErroApi(
      response.status,
      messages[response.status] ??
        "Serviço indisponível. Tente novamente em instantes.",
    );
  }
  return response.status === 204
    ? (undefined as T)
    : (response.json().catch(() => {
        throw new ErroApi(
          502,
          "Não foi possível ler a resposta. Tente novamente em instantes.",
        );
      }) as Promise<T>);
}
let csrf: Promise<string> | undefined;
export function limparCsrf() {
  csrf = undefined;
}
export function obterCsrf(): Promise<string> {
  return (csrf ??= fetch(`${urlApiJava}/api/v1/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  })
    .then(
      lerResposta<{
        token: string;
      }>,
    )
    .then((body) => {
      if (!body.token) throw new Error("Não foi possível preparar a operação.");
      return body.token;
    })
    .catch((error) => {
      limparCsrf();
      throw error instanceof TypeError
        ? new ErroApi(
            0,
            "Não foi possível conectar. Tente novamente em instantes.",
          )
        : error;
    }));
}
export async function requisitarJava<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (method !== "GET") headers["X-XSRF-TOKEN"] = await obterCsrf();
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(`${urlApiJava}/api/v1${path}`, {
    method,
    headers,
    credentials: "include",
    cache: "no-store",
    body: body === undefined ? undefined : JSON.stringify(body),
  }).catch(() => {
    throw new ErroApi(
      0,
      "Não foi possível conectar. Tente novamente em instantes.",
    );
  });
  // Renovamos o CSRF na próxima ação explícita, sem repetir mutações.
  if (response.status === 401 || response.status === 403) limparCsrf();
  if (
    response.status === 401 &&
    path !== "/auth/login" &&
    typeof window !== "undefined"
  )
    window.dispatchEvent(new Event("java-session-expired"));
  return lerResposta<T>(response);
}
export const consultarSaudeJava = () =>
  requisitarJava<{
    status: "UP";
  }>("/health");
