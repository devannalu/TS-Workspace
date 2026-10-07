import { requisitarJava } from "./http";
import type { ReferenciaTarefa, PrioridadeTarefa } from "./tarefas";
export type TipoCalendario = "TAREFA" | "PROJETO";
export type ItemCalendario = {
  id: string; tipo: TipoCalendario; recursoId: string; titulo: string;
  dataInicio: string; dataFim: string; equipe: ReferenciaTarefa; status: string;
  prioridade: PrioridadeTarefa | null; responsaveis: ReferenciaTarefa[]; concluido: boolean; atrasado: boolean;
};
export function buscarCalendario(de: string, ate: string, equipeId = "", tipo = "", responsavelId = "") {
  const parametros = new URLSearchParams({ from: de, to: ate });
  if (equipeId) parametros.set("teamId", equipeId);
  if (tipo) parametros.set("types", tipo);
  if (responsavelId) parametros.set("responsibleId", responsavelId);
  return requisitarJava<ItemCalendario[]>(`/calendar?${parametros}`);
}
