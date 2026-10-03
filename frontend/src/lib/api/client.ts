import { javaRequest } from "./http";
export const getJavaHealth=()=>javaRequest<{status:"UP"}>("/health");
