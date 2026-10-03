export { javaRequest as managementRequest } from "./http";
export type JavaPage<T> = { items: T[]; total: number; page: number; size: number };
export type JavaRoleRef = { id: string; key: "SUPER_ADMIN" | "ADMIN" | "SUPERVISOR" | "SUPPORT"; name: string };
export type JavaTeamRef = { id: string; name: string };
