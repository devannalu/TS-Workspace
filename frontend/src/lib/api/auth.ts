const javaApiUrl = process.env.NEXT_PUBLIC_JAVA_API_URL ?? "http://localhost:8080";

export type JavaUser = {
  id: string;
  name: string;
  email: string;
  jobTitle: string | null;
  status: "ACTIVE" | "INACTIVE";
  role: { key: "SUPER_ADMIN" | "ADMIN" | "SUPERVISOR" | "SUPPORT"; name: string };
  permissions: string[];
};

type CsrfResponse = { token: string };

async function parseResponse<T>(response: Response): Promise<T> {
  if (!response.ok) throw new Error("Não foi possível concluir a operação de autenticação.");
  return response.json() as Promise<T>;
}

export async function getCsrf(): Promise<string> {
  const response = await fetch(`${javaApiUrl}/api/v1/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });
  const body = await parseResponse<CsrfResponse>(response);
  if (!body.token) throw new Error("Token CSRF ausente.");
  return body.token;
}

export async function loginJava(email: string, password: string): Promise<JavaUser> {
  const token = await getCsrf();
  const response = await fetch(`${javaApiUrl}/api/v1/auth/login`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": token },
    body: JSON.stringify({ email, password }),
  });
  return parseResponse<JavaUser>(response);
}

export async function logoutJava(): Promise<void> {
  const token = await getCsrf();
  const response = await fetch(`${javaApiUrl}/api/v1/auth/logout`, {
    method: "POST",
    credentials: "include",
    headers: { "X-XSRF-TOKEN": token },
  });
  if (!response.ok && response.status !== 401) throw new Error("Não foi possível encerrar a sessão Java.");
}

export async function getCurrentJavaUser(): Promise<JavaUser | null> {
  const response = await fetch(`${javaApiUrl}/api/v1/auth/me`, {
    credentials: "include",
    cache: "no-store",
  });
  if (response.status === 401) return null;
  return parseResponse<JavaUser>(response);
}
