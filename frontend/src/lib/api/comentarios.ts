import { requisitarJava } from "./http";

export type RecursoComentario = "tasks" | "projects";
export type PessoaComentario = { id: string; nome: string };
export type Comentario = {
  id: string;
  autora: PessoaComentario;
  conteudo: string | null;
  editado: boolean;
  removido: boolean;
  versao: number;
  criadaEm: string;
  atualizadaEm: string;
  capacidades: { editar: boolean; remover: boolean };
};
export type PaginaComentarios = {
  items: Comentario[];
  total: number;
  page: number;
  size: number;
  podeComentar: boolean;
};
export type Atividade = {
  id: string;
  tipo: string;
  ator: PessoaComentario | null;
  criadaEm: string;
};
export type PaginaAtividade = {
  items: Atividade[];
  total: number;
  page: number;
  size: number;
};
export const conflitoComentario =
  "Este comentário foi alterado. Atualize os dados e tente novamente.";
export const listarComentarios = (
  recurso: RecursoComentario,
  id: string,
  pagina = 0,
) =>
  requisitarJava<PaginaComentarios>(
    `/${recurso}/${encodeURIComponent(id)}/comments?page=${pagina}&size=25`,
  );
export const criarComentario = (
  recurso: RecursoComentario,
  id: string,
  conteudo: string,
) =>
  requisitarJava<Comentario>(
    `/${recurso}/${encodeURIComponent(id)}/comments`,
    "POST",
    { conteudo },
  );
export const editarComentario = (comentario: Comentario, conteudo: string) =>
  requisitarJava<Comentario>(
    `/comments/${encodeURIComponent(comentario.id)}`,
    "PATCH",
    { conteudo, versao: comentario.versao },
  );
export const removerComentario = (comentario: Comentario) =>
  requisitarJava<Comentario>(
    `/comments/${encodeURIComponent(comentario.id)}/remove`,
    "POST",
    { versao: comentario.versao },
  );
export const listarAtividade = (
  recurso: RecursoComentario,
  id: string,
  pagina = 0,
) =>
  requisitarJava<PaginaAtividade>(
    `/${recurso}/${encodeURIComponent(id)}/activity?page=${pagina}&size=25`,
  );

export const mensagensAtividade: Record<string, string> = {
  "task.created": "criou a tarefa.",
  "task.updated": "atualizou a tarefa.",
  "task.status_changed": "alterou o status da tarefa.",
  "task.assignees_changed": "atualizou as responsáveis da tarefa.",
  "task.archived": "arquivou a tarefa.",
  "project.created": "criou o projeto.",
  "project.updated": "atualizou o projeto.",
  "project.status_changed": "alterou o status do projeto.",
  "project.responsibles_changed": "atualizou as responsáveis do projeto.",
  "project.archived": "arquivou o projeto.",
  "comment.created": "adicionou um comentário.",
  "comment.updated": "editou um comentário.",
  "comment.removed": "removeu um comentário.",
};

export function formatarDataColaboracao(data: string) {
  const instante = new Date(data),
    hoje = new Date(),
    ontem = new Date();
  ontem.setDate(ontem.getDate() - 1);
  const dia = (d: Date) => d.toLocaleDateString("pt-BR");
  const hora = instante.toLocaleTimeString("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
  });
  if (dia(instante) === dia(hoje)) return `Hoje, ${hora}`;
  if (dia(instante) === dia(ontem)) return `Ontem, ${hora}`;
  return `${instante.toLocaleDateString("pt-BR", { day: "numeric", month: "short", year: "numeric" })}, ${hora}`;
}
