import { ApiError, clearCsrf, javaRequest } from "./http";
export { getCsrf } from "./http";
export type JavaUser = {
 id: string; name: string; email: string; jobTitle: string | null;
 status: "ACTIVE" | "INACTIVE";
 role: { id: string; key: "SUPER_ADMIN" | "ADMIN" | "SUPERVISOR" | "SUPPORT"; name: string };
 permissions: string[];
};
export async function loginJava(email: string, password: string) {
 try { return await javaRequest<JavaUser>("/auth/login", "POST", {email,password}); }
 finally { clearCsrf(); }
}
export async function logoutJava() {
 try { await javaRequest<void>("/auth/logout", "POST"); }
 finally { clearCsrf(); }
}
export async function getCurrentJavaUser(): Promise<JavaUser | null> {
 try { return await javaRequest<JavaUser>("/auth/me"); }
 catch (error) { if(error instanceof ApiError && error.status===401) return null; throw error; }
}
