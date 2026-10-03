export function formatarData(valor: string) {
  const data = new Date(valor);
  return Number.isNaN(data.getTime())
    ? "Data indisponível"
    : new Intl.DateTimeFormat("pt-BR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        timeZone: "America/Bahia",
      }).format(data);
}
export const rotulosSituacaoConvite = {
  PENDING: "Pendente",
  USED: "Usado",
  CANCELLED: "Cancelado",
  EXPIRED: "Expirado",
} as const;
export function normalizarPagina(valor: unknown) {
  const numero = typeof valor === "string" ? Number(valor) : 0;
  return Number.isInteger(numero) && numero >= 0 && numero <= 100000 ? numero : 0;
}
