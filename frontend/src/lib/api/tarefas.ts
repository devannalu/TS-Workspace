import { ErroApi, requisitarJava } from "./http";
import type { PaginaApi } from "./contratos";

export const statusTarefa = {
  A_FAZER: "A fazer",
  EM_ANDAMENTO: "Em andamento",
  EM_REVISAO: "Em revisão",
  CONCLUIDA: "Concluída",
} as const;
export const prioridadesTarefa = {
  BAIXA: "Baixa",
  MEDIA: "Média",
  ALTA: "Alta",
  URGENTE: "Urgente",
} as const;
export type StatusTarefa = keyof typeof statusTarefa;
export type PrioridadeTarefa = keyof typeof prioridadesTarefa;
export type ReferenciaTarefa = { id: string; nome: string };
export type Tarefa = {
  id: string;
  titulo: string;
  descricao: string | null;
  status: StatusTarefa;
  prioridade: PrioridadeTarefa;
  prazo: string | null;
  ordem: number;
  versao: number;
  equipe: ReferenciaTarefa;
  criadaPor: ReferenciaTarefa;
  responsaveis: ReferenciaTarefa[];
  criadaEm: string;
  atualizadaEm: string;
  arquivada: boolean;
  atrasada: boolean;
  capacidades: { editar: boolean; atribuir: boolean; arquivar: boolean };
  projeto?: ReferenciaTarefa | null;
};
export type DadosTarefa = {
  titulo: string;
  descricao: string | null;
  prioridade: PrioridadeTarefa;
  equipeId: string;
  prazo: string | null;
  responsavelIds?: string[];
  projetoId?: string | null;
};
export type OpcoesTarefas = {
  equipes: ReferenciaTarefa[];
  responsaveis: ReferenciaTarefa[];
};
export type ResumoTarefas = {
  minhasTarefas: number;
  emAndamento: number;
  vencendoHoje: number;
  atrasadas: number;
};
export type FiltrosTarefas = {
  teamId?: string;
  status?: StatusTarefa;
  priority?: PrioridadeTarefa;
  assigneeId?: string;
  search?: string;
  dueFrom?: string;
  dueTo?: string;
  archived?: boolean;
  projectId?: string;
};
export const mensagemConflitoTarefa =
  "Esta tarefa foi atualizada por outra pessoa. Atualize os dados e tente novamente.";
async function alterarTarefa<T>(
  caminho: string,
  metodo: string,
  dados: unknown,
): Promise<T> {
  try {
    return await requisitarJava<T>(caminho, metodo, dados);
  } catch (erro) {
    if (
      erro instanceof ErroApi &&
      erro.status === 409 &&
      erro.message === "A operação conflita com o estado atual do workspace."
    )
      throw new ErroApi(409, mensagemConflitoTarefa);
    throw erro;
  }
}
export function parametrosTarefas(filtros: FiltrosTarefas, pagina = 0) {
  const parametros = new URLSearchParams({ page: String(pagina), size: "25" });
  Object.entries(filtros).forEach(([chave, valor]) => {
    if (valor !== undefined && valor !== "")
      parametros.set(chave, String(valor));
  });
  return parametros.toString();
}
export const listarTarefas = (filtros: FiltrosTarefas, pagina = 0) =>
  requisitarJava<PaginaApi<Tarefa>>(
    `/tasks?${parametrosTarefas(filtros, pagina)}`,
  );
export const buscarTarefa = (id: string) =>
  requisitarJava<Tarefa>(`/tasks/${id}`);
export const buscarOpcoesTarefas = (equipeId?: string) =>
  requisitarJava<OpcoesTarefas>(
    `/tasks/options${equipeId ? `?teamId=${encodeURIComponent(equipeId)}` : ""}`,
  );
export const criarTarefa = (dados: DadosTarefa) =>
  alterarTarefa<Tarefa>("/tasks", "POST", dados);
export const editarTarefa = (tarefa: Tarefa, dados: DadosTarefa) =>
  alterarTarefa<Tarefa>(`/tasks/${tarefa.id}`, "PATCH", {
    ...dados,
    versao: tarefa.versao,
  });
export const moverTarefa = (
  tarefa: Tarefa,
  status: StatusTarefa,
  antesDeId?: string,
) =>
  alterarTarefa<Tarefa>(`/tasks/${tarefa.id}/position`, "PATCH", {
    status,
    antesDeId,
    versao: tarefa.versao,
  });
export const arquivarTarefa = (tarefa: Tarefa) =>
  alterarTarefa<Tarefa>(`/tasks/${tarefa.id}/archive`, "POST", {
    versao: tarefa.versao,
  });
export function formatarPrazoTarefa(prazo: string) {
  const [ano, mes, dia] = prazo.split("-");
  return `${dia}/${mes}/${ano}`;
}
