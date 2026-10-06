"use client";
import { useSortable } from "@dnd-kit/sortable";
import { GripVertical } from "lucide-react";
import {
  formatarPrazoTarefa,
  prioridadesTarefa,
  statusTarefa,
  type StatusTarefa,
  type Tarefa,
} from "@/lib/api/tarefas";
import { Avatar } from "../ui/avatar";

export function ConteudoCardTarefa({ tarefa }: { tarefa: Tarefa }) {
  return (
    <>
      <span
        className={`task-priority priority-${tarefa.prioridade.toLowerCase()}`}
      >
        Prioridade {prioridadesTarefa[tarefa.prioridade]}
      </span>
      <h3 className="mt-3 break-words font-semibold">{tarefa.titulo}</h3>
      {tarefa.projeto && (
        <p className="mt-1 break-words text-xs text-primary">
          Projeto · {tarefa.projeto.nome}
        </p>
      )}
      <p className="mt-2 break-words text-xs text-muted-foreground">
        {tarefa.equipe.nome}
      </p>
      {tarefa.prazo && (
        <p
          className={`mt-3 text-xs ${tarefa.atrasada ? "font-semibold text-danger" : "text-muted-foreground"}`}
        >
          {tarefa.atrasada ? "Atrasada · " : "Prazo · "}
          <time dateTime={tarefa.prazo}>
            {formatarPrazoTarefa(tarefa.prazo)}
          </time>
        </p>
      )}
      <div
        className="mt-3 flex items-center gap-1"
        aria-label={
          tarefa.responsaveis.length
            ? `Responsáveis: ${tarefa.responsaveis.map((pessoa) => pessoa.nome).join(", ")}`
            : "Sem responsáveis"
        }
      >
        {tarefa.responsaveis.slice(0, 3).map((pessoa) => (
          <Avatar key={pessoa.id} name={pessoa.nome} />
        ))}
        {tarefa.responsaveis.length > 3 && (
          <span className="text-xs">+{tarefa.responsaveis.length - 3}</span>
        )}
        {!tarefa.responsaveis.length && (
          <span className="text-xs text-muted-foreground">
            Sem responsáveis
          </span>
        )}
      </div>
    </>
  );
}
export function CardTarefa({
  tarefa,
  arrasteAtivo,
  ocupada,
  aoAbrir,
  aoMover,
}: {
  tarefa: Tarefa;
  arrasteAtivo: boolean;
  ocupada: boolean;
  aoAbrir: (tarefa: Tarefa) => void;
  aoMover: (tarefa: Tarefa, status: StatusTarefa) => void;
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: tarefa.id,
    disabled: !arrasteAtivo || !tarefa.capacidades.editar || ocupada,
  });
  return (
    <article
      ref={setNodeRef}
      className={`task-card ${isDragging ? "opacity-30" : ""}`}
      style={{
        transform: transform
          ? `translate3d(${transform.x}px,${transform.y}px,0)`
          : undefined,
        transition,
      }}
    >
      {arrasteAtivo && tarefa.capacidades.editar && (
        <button
          type="button"
          id={`arrastar-${tarefa.id}`}
          className="task-drag"
          aria-label={`Arrastar ${tarefa.titulo}`}
          disabled={ocupada}
          style={{ touchAction: "none" }}
          {...attributes}
          {...listeners}
        >
          <GripVertical size={18} aria-hidden />
        </button>
      )}
      <button
        type="button"
        disabled={ocupada}
        className="block w-full text-left"
        onClick={() => aoAbrir(tarefa)}
        aria-label={`Abrir tarefa ${tarefa.titulo}`}
      >
        <ConteudoCardTarefa tarefa={tarefa} />
      </button>
      {tarefa.capacidades.editar && (
        <label className="mt-3 block text-xs text-muted-foreground">
          Mover para…
          <select
            id={`mover-${tarefa.id}`}
            aria-label={`Mover ${tarefa.titulo} para`}
            className="task-select mt-1 w-full"
            value={tarefa.status}
            disabled={ocupada}
            onChange={(evento) =>
              aoMover(tarefa, evento.target.value as StatusTarefa)
            }
          >
            {Object.entries(statusTarefa).map(([valor, texto]) => (
              <option key={valor} value={valor}>
                {texto}
              </option>
            ))}
          </select>
        </label>
      )}
    </article>
  );
}
