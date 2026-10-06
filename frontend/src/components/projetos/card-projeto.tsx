import { statusProjeto, type Projeto } from "@/lib/api/projetos";
import { formatarPrazoTarefa } from "@/lib/api/tarefas";
import { Avatar } from "../ui/avatar";
import { Badge } from "../ui/badge";

export function ProgressoProjeto({ projeto }: { projeto: Projeto }) {
  if (!projeto.totalTarefas) return <p className="subtle">Sem tarefas</p>;
  return (
    <div className="space-y-2">
      <div className="flex justify-between gap-2 text-sm">
        <span>
          {projeto.tarefasConcluidas} de {projeto.totalTarefas} tarefas
          concluídas
        </span>
        <strong>{projeto.percentualProgresso}%</strong>
      </div>
      <div
        role="progressbar"
        aria-label={`Progresso de ${projeto.titulo}`}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={projeto.percentualProgresso ?? 0}
        className="h-2 overflow-hidden rounded-full bg-muted"
      >
        <div
          className="h-full rounded-full bg-primary"
          style={{ width: `${projeto.percentualProgresso ?? 0}%` }}
        />
      </div>
    </div>
  );
}
export function CardProjeto({
  projeto,
  aoAbrir,
}: {
  projeto: Projeto;
  aoAbrir: () => void;
}) {
  return (
    <article className="min-w-0 rounded-2xl border border-border bg-card shadow-sm">
      <button
        type="button"
        onClick={aoAbrir}
        className="w-full space-y-4 rounded-2xl p-5 text-left transition-colors hover:bg-muted/40"
        aria-label={`Abrir projeto ${projeto.titulo}`}
      >
        <div className="flex flex-wrap items-center gap-2">
          <Badge
            tone={
              projeto.status === "CONCLUIDO"
                ? "success"
                : projeto.status === "EM_ANDAMENTO"
                  ? "pink"
                  : "neutral"
            }
          >
            {statusProjeto[projeto.status]}
          </Badge>
          {projeto.arquivado && <Badge>Arquivado</Badge>}
        </div>
        <div>
          <h2 className="break-words text-lg font-semibold">
            {projeto.titulo}
          </h2>
          <p className="mt-1 subtle">{projeto.equipe.nome}</p>
        </div>
        <p className="line-clamp-2 min-h-10 break-words text-sm text-muted-foreground">
          {projeto.descricao || "Um objetivo para construir juntas."}
        </p>
        <ProgressoProjeto projeto={projeto} />
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div
            className="flex items-center gap-1"
            aria-label={
              projeto.responsaveis.length
                ? `Responsáveis: ${projeto.responsaveis.map((p) => p.nome).join(", ")}`
                : "Sem responsáveis"
            }
          >
            {projeto.responsaveis.slice(0, 3).map((p) => (
              <Avatar key={p.id} name={p.nome} />
            ))}
            {projeto.responsaveis.length > 3 && (
              <span className="text-xs">
                +{projeto.responsaveis.length - 3}
              </span>
            )}
            {!projeto.responsaveis.length && (
              <span className="subtle">Sem responsáveis</span>
            )}
          </div>
          <p className="text-xs text-muted-foreground">
            {projeto.dataInicio
              ? formatarPrazoTarefa(projeto.dataInicio)
              : "Sem início"}{" "}
            —{" "}
            {projeto.dataFim
              ? formatarPrazoTarefa(projeto.dataFim)
              : "Sem prazo"}
          </p>
        </div>
      </button>
    </article>
  );
}
