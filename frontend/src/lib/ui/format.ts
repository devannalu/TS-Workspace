export function friendlyDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "Data indisponível"
    : new Intl.DateTimeFormat("pt-BR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        timeZone: "America/Bahia",
      }).format(date);
}
export const inviteLabels = {
  PENDING: "Pendente",
  USED: "Usado",
  CANCELLED: "Cancelado",
  EXPIRED: "Expirado",
} as const;
export function pageNumber(value: unknown) {
  const n = typeof value === "string" ? Number(value) : 0;
  return Number.isInteger(n) && n >= 0 && n <= 100000 ? n : 0;
}
