"use client";
import { useId, useRef, useState, type ReactNode } from "react";
import type { RecursoComentario } from "@/lib/api/comentarios";
import { Anexos } from "@/components/anexos/anexos";
import { Comentarios } from "./comentarios";
import { LinhaAtividade } from "./linha-atividade";

export function AbasColaboracao({
  recurso,
  recursoId,
  children,
}: {
  recurso: RecursoComentario;
  recursoId: string;
  children: ReactNode;
}) {
  const id = useId(),
    botoes = useRef<(HTMLButtonElement | null)[]>([]);
  const [aba, definirAba] = useState(0);
  return (
    <div className="space-y-4">
      <div
        role="tablist"
        aria-label="Informações do recurso"
        className="flex gap-1 border-b border-border"
      >
        {["Detalhes", "Comentários", "Anexos", "Atividade"].map((nome, indice) => (
          <button
            key={nome}
            ref={(el) => {
              botoes.current[indice] = el;
            }}
            type="button"
            role="tab"
            id={`${id}-aba-${indice}`}
            aria-controls={`${id}-painel-${indice}`}
            aria-selected={aba === indice}
            tabIndex={aba === indice ? 0 : -1}
            className={`min-h-11 min-w-0 flex-auto rounded-t-lg px-1 text-xs font-medium sm:px-2 sm:text-sm ${aba === indice ? "border-b-2 border-primary bg-muted" : "subtle"}`}
            onClick={() => definirAba(indice)}
            onKeyDown={(e) => {
              let proxima: number;
              if (e.key === "ArrowRight") proxima = (indice + 1) % 4;
              else if (e.key === "ArrowLeft") proxima = (indice + 3) % 4;
              else if (e.key === "Home") proxima = 0;
              else if (e.key === "End") proxima = 3;
              else return;
              e.preventDefault();
              definirAba(proxima);
              botoes.current[proxima]?.focus();
            }}
          >
            {nome}
          </button>
        ))}
      </div>
      <div
        className="space-y-4"
        role="tabpanel"
        id={`${id}-painel-${aba}`}
        aria-labelledby={`${id}-aba-${aba}`}
        tabIndex={0}
      >
        {aba === 0 ? (
          children
        ) : aba === 1 ? (
          <Comentarios recurso={recurso} recursoId={recursoId} />
        ) : aba === 2 ? (
          <Anexos recurso={recurso} recursoId={recursoId} />
        ) : (
          <LinhaAtividade recurso={recurso} recursoId={recursoId} />
        )}
      </div>
    </div>
  );
}
