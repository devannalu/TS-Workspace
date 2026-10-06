import { requisitarJava } from "./http";
import type { PaginaApi } from "./contratos";
import type { ReferenciaTarefa } from "./tarefas";

export const statusProjeto = {
  PLANEJADO: "Planejado",
  EM_ANDAMENTO: "Em andamento",
  PAUSADO: "Pausado",
  CONCLUIDO: "Concluído",
} as const;
export type StatusProjeto = keyof typeof statusProjeto;
export type Projeto = {
  id: string;
  titulo: string;
  descricao: string | null;
  status: StatusProjeto;
  equipe: ReferenciaTarefa;
  responsaveis: ReferenciaTarefa[];
  criadaPor: ReferenciaTarefa;
  dataInicio: string | null;
  dataFim: string | null;
  versao: number;
  arquivado: boolean;
  criadaEm: string;
  atualizadaEm: string;
  totalTarefas: number;
  tarefasConcluidas: number;
  percentualProgresso: number | null;
  emAndamento: number;
  emRevisao: number;
  aFazer: number;
  capacidades: {
    editar: boolean;
    gerenciarResponsaveis: boolean;
    arquivar: boolean;
    trocarEquipe: boolean;
  };
};
export type DadosProjeto = {
  titulo: string;
  descricao: string | null;
  equipeId: string;
  responsavelIds?: string[];
  dataInicio: string | null;
  dataFim: string | null;
  status?: StatusProjeto;
};
export type OpcoesProjetos = {
  equipes: ReferenciaTarefa[];
  responsaveis: ReferenciaTarefa[];
};
export type ResumoProjetos = {
  projetosAtivos: number;
  emAndamento: number;
  comPrazoProximo: number;
};
export type FiltrosProjetos = {
  teamId?: string;
  status?: StatusProjeto;
  responsibleId?: string;
  search?: string;
  startFrom?: string;
  startTo?: string;
  dueFrom?: string;
  dueTo?: string;
  archived?: boolean;
};
export const mensagemConflitoProjeto =
  "Este projeto foi atualizado por outra pessoa. Atualize os dados e tente novamente.";
export function parametrosProjetos(filtros: FiltrosProjetos, pagina = 0) {
  const parametros = new URLSearchParams({ page: String(pagina), size: "24" });
  Object.entries(filtros).forEach(([chave, valor]) => {
    if (valor !== undefined && valor !== "")
      parametros.set(chave, String(valor));
  });
  return parametros.toString();
}
export const listarProjetos = (filtros: FiltrosProjetos, pagina = 0) =>
  requisitarJava<PaginaApi<Projeto>>(
    `/projects?${parametrosProjetos(filtros, pagina)}`,
  );
export const buscarProjeto = (id: string) =>
  requisitarJava<Projeto>(`/projects/${id}`);
export const buscarOpcoesProjetos = (equipeId?: string) =>
  requisitarJava<OpcoesProjetos>(
    `/projects/options${equipeId ? `?teamId=${encodeURIComponent(equipeId)}` : ""}`,
  );
export const criarProjeto = (dados: DadosProjeto) =>
  requisitarJava<Projeto>("/projects", "POST", dados);
export const editarProjeto = (projeto: Projeto, dados: DadosProjeto) =>
  requisitarJava<Projeto>(`/projects/${projeto.id}`, "PATCH", {
    ...dados,
    versao: projeto.versao,
  });
export const arquivarProjeto = (projeto: Projeto) =>
  requisitarJava<Projeto>(`/projects/${projeto.id}/archive`, "POST", {
    versao: projeto.versao,
  });
