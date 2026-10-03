export type PaginaApi<T> = {
  items: T[];
  total: number;
  page: number;
  size: number;
};
export type PerfilAcessoReferencia = {
  id: string;
  key: "SUPER_ADMIN" | "ADMIN" | "SUPERVISOR" | "SUPPORT";
  name: string;
};
export type EquipeReferencia = {
  id: string;
  name: string;
};
