"use client";
import { useEffect, useState } from "react";
import {
  listarAtividade,
  mensagensAtividade,
  formatarDataColaboracao,
  type PaginaAtividade,
  type RecursoComentario,
} from "@/lib/api/comentarios";
import { Button } from "../ui/button";
import { ErrorState, Skeleton } from "../ui/feedback";

export function LinhaAtividade({
  recurso,
  recursoId,
}: {
  recurso: RecursoComentario;
  recursoId: string;
}) {
  const [pagina, definirPagina] = useState<PaginaAtividade | null>(null);
  const [numero, definirNumero] = useState(0),
    [tentativa, definirTentativa] = useState(0);
  const [erro, definirErro] = useState(""),
    [carregando, definirCarregando] = useState(true);
  useEffect(() => {
    let cancelada = false;
    listarAtividade(recurso, recursoId, numero)
      .then((dados) => {
        if (!cancelada) definirPagina(dados);
      })
      .catch((e) => {
        if (!cancelada)
          definirErro(
            e instanceof Error
              ? e.message
              : "Não foi possível carregar a atividade.",
          );
      })
      .finally(() => {
        if (!cancelada) definirCarregando(false);
      });
    return () => {
      cancelada = true;
    };
  }, [recurso, recursoId, numero, tentativa]);
  return (
    <section aria-label="Histórico de atividade" className="space-y-4">
      {carregando ? (
        <Skeleton />
      ) : erro ? (
        <ErrorState
          message={erro}
          onRetry={() => {
            definirCarregando(true);
            definirErro("");
            definirTentativa((n) => n + 1);
          }}
        />
      ) : (
        pagina && (
          <>
            {!pagina.items.length && (
              <p className="py-6 text-center subtle">
                Ainda não há atividades registradas.
              </p>
            )}
            <ol className="space-y-4 border-l border-border pl-4">
              {pagina.items.map((evento) => (
                <li key={evento.id} className="relative text-sm">
                  <span
                    aria-hidden
                    className="absolute -left-[1.3rem] top-1 size-2 rounded-full bg-primary"
                  />
                  <p className="break-words">
                    <span className="font-medium">
                      {evento.ator?.nome ?? "Sistema"}
                    </span>{" "}
                    {mensagensAtividade[evento.tipo] ??
                      "registrou uma atividade."}
                  </p>
                  <time
                    className="subtle text-xs"
                    dateTime={evento.criadaEm}
                    title={new Date(evento.criadaEm).toLocaleString("pt-BR")}
                  >
                    {formatarDataColaboracao(evento.criadaEm)}
                  </time>
                </li>
              ))}
            </ol>
            {pagina.total > pagina.size && (
              <div className="flex flex-wrap items-center gap-2">
                <Button
                  variant="secondary"
                  disabled={!numero}
                  onClick={() => {
                    definirCarregando(true);
                    definirNumero((n) => n - 1);
                  }}
                >
                  Atividades mais recentes
                </Button>
                <span className="subtle text-sm">Página {numero + 1}</span>
                <Button
                  variant="secondary"
                  disabled={(numero + 1) * pagina.size >= pagina.total}
                  onClick={() => {
                    definirCarregando(true);
                    definirNumero((n) => n + 1);
                  }}
                >
                  Atividades mais antigas
                </Button>
              </div>
            )}
          </>
        )
      )}
    </section>
  );
}
