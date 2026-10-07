export type VisaoCalendario = "month" | "week" | "agenda";

// UTC é usado somente como aritmética interna; transporte e apresentação continuam date-only.
function numeroData(data: string) {
  const [ano, mes, dia] = data.split("-").map(Number);
  return Date.UTC(ano, mes - 1, dia);
}
export function dataValida(data: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(data) && data >= "1900-01-01" && data <= "9998-12-31"
    && new Date(numeroData(data)).toISOString().slice(0, 10) === data;
}
export function hojeLocal() {
  const hoje = new Date();
  return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, "0")}-${String(hoje.getDate()).padStart(2, "0")}`;
}
export function somarDias(data: string, quantidade: number) {
  return new Date(numeroData(data) + quantidade * 86400000).toISOString().slice(0, 10);
}
export function inicioSemana(data: string) {
  const dia = new Date(numeroData(data)).getUTCDay();
  return somarDias(data, -((dia + 6) % 7));
}
export function intervaloVisivel(data: string, visao: VisaoCalendario) {
  if (visao === "week") {
    const de = inicioSemana(data);
    return { de, ate: somarDias(de, 6) };
  }
  const primeiro = data.slice(0, 7) + "-01";
  const [ano, mes] = data.split("-").map(Number);
  const ultimo = new Date(Date.UTC(ano, mes, 0)).toISOString().slice(0, 10);
  if (visao === "agenda") return { de: primeiro, ate: ultimo };
  return { de: inicioSemana(primeiro), ate: somarDias(inicioSemana(ultimo), 6) };
}
export function navegarPeriodo(data: string, visao: VisaoCalendario, direcao: number) {
  if (visao === "week") return somarDias(data, 7 * direcao);
  const [ano, mes] = data.split("-").map(Number);
  return new Date(Date.UTC(ano, mes - 1 + direcao, 1)).toISOString().slice(0, 10);
}
export function diasIntervalo(de: string, ate: string) {
  const dias: string[] = [];
  for (let dia = de; dia <= ate; dia = somarDias(dia, 1)) dias.push(dia);
  return dias;
}
export function formatarData(data: string, opcoes: Intl.DateTimeFormatOptions = { day: "numeric", month: "long", year: "numeric" }) {
  return new Intl.DateTimeFormat("pt-BR", { ...opcoes, timeZone: "UTC" }).format(new Date(numeroData(data)));
}
