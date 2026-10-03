import { requisitarJava } from "./http";
import type { PerfilAcessoReferencia, EquipeReferencia } from "./contratos";
export type Convite = {
  id: string;
  email: string;
  role: PerfilAcessoReferencia;
  teams: EquipeReferencia[];
  invitedBy: {
    id: string;
    name: string;
  };
  createdAt: string;
  expiresAt: string;
  status: "PENDING" | "USED" | "CANCELLED" | "EXPIRED";
};
export type ConviteCriadoResponse = {
  invite: Convite;
  token: string;
  inviteUrl: string;
};
export type CriarConviteRequest = {
  email: string;
  roleId: string;
  teamIds: string[];
  expiresInDays?: number;
};
export type ConvitePublicoResponse = {
  email: string;
  role: string;
  teams: string[];
  expiresAt: string;
};
export type AceitarConviteRequest = {
  token: string;
  name: string;
  password: string;
  passwordConfirmation: string;
};
export const criarConvite = (input: CriarConviteRequest) => requisitarJava<ConviteCriadoResponse>("/invites", "POST", input);
export const cancelarConvite = (id: string) => requisitarJava<Convite>(`/invites/${encodeURIComponent(id)}/cancel`, "POST");
export const consultarConvitePublico = (token: string) => requisitarJava<ConvitePublicoResponse>("/invites/validate", "POST", { token });
export const aceitarConvite = (input: AceitarConviteRequest) => requisitarJava<{
  userId: string;
  inviteId: string;
}>("/invites/accept", "POST", input);
