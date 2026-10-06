"use client";
import { useEffect, useId, useRef, useState } from "react";
import {
  criarComentario,
  listarComentarios,
  type Comentario,
  type PaginaComentarios,
  type RecursoComentario,
} from "@/lib/api/comentarios";
import { Button } from "../ui/button";
import { Textarea } from "../ui/textarea";
import { ErrorState, Skeleton } from "../ui/feedback";
import { ComentarioItem } from "./comentario-item";

export function Comentarios({
  recurso,
  recursoId,
}: {
  recurso: RecursoComentario;
  recursoId: string;
}) {
  const campo = useId(),
    anteriores = useRef<HTMLButtonElement>(null);
  const [pagina, definirPagina] = useState<PaginaComentarios | null>(null),
    [itens, definirItens] = useState<Comentario[]>([]);
  const [texto, definirTexto] = useState(""),
    [erro, definirErro] = useState(""),
    [erroEnvio, definirErroEnvio] = useState("");
  const [carregando, definirCarregando] = useState(true),
    [enviando, definirEnviando] = useState(false),
    [status, definirStatus] = useState("");
  const geracao = useRef(0);
  async function carregar(numero = 0) {
    const atual = ++geracao.current;
    definirCarregando(true);
    definirErro("");
    const topo = anteriores.current?.getBoundingClientRect().top;
    try {
      const dados = await listarComentarios(recurso, recursoId, numero);
      if (atual !== geracao.current) return;
      definirPagina(dados);
      definirItens((anteriores) =>
        numero === 0
          ? dados.items
          : [...anteriores, ...dados.items].filter(
              (item, indice, todos) =>
                todos.findIndex((c) => c.id === item.id) === indice,
            ),
      );
      if (numero > 0 && topo !== undefined)
        requestAnimationFrame(() =>
          anteriores.current?.scrollIntoView({ block: "nearest" }),
        );
    } catch (e) {
      if (atual === geracao.current)
        definirErro(
          e instanceof Error
            ? e.message
            : "Não foi possível carregar os comentários.",
        );
      throw e;
    } finally {
      if (atual === geracao.current) definirCarregando(false);
    }
  }
  useEffect(() => {
    let cancelada = false;
    listarComentarios(recurso, recursoId)
      .then((dados) => {
        if (cancelada) return;
        definirPagina(dados);
        definirItens(dados.items);
        definirCarregando(false);
      })
      .catch((e) => {
        if (!cancelada) {
          definirErro(
            e instanceof Error
              ? e.message
              : "Não foi possível carregar os comentários.",
          );
          definirCarregando(false);
        }
      });
    return () => {
      cancelada = true;
    };
  }, [recurso, recursoId]);
  async function recarregar() {
    const paginas = await Promise.all(
      Array.from({ length: (pagina?.page ?? 0) + 1 }, (_, n) =>
        listarComentarios(recurso, recursoId, n),
      ),
    );
    definirPagina(paginas.at(-1)!);
    definirItens(
      paginas
        .flatMap((p) => p.items)
        .filter(
          (item, indice, todos) =>
            todos.findIndex((c) => c.id === item.id) === indice,
        ),
    );
  }
  async function enviar() {
    definirEnviando(true);
    definirErroEnvio("");
    definirStatus("");
    try {
      const comentario = await criarComentario(recurso, recursoId, texto);
      definirItens((atuais) => [comentario, ...atuais]);
      definirPagina((atual) =>
        atual ? { ...atual, total: atual.total + 1 } : atual,
      );
      definirTexto("");
      definirStatus("Comentário adicionado.");
    } catch (e) {
      definirErroEnvio(
        e instanceof Error
          ? e.message
          : "Não foi possível enviar o comentário.",
      );
    } finally {
      definirEnviando(false);
    }
  }
  function atualizar(comentario: Comentario) {
    definirItens((atuais) =>
      atuais.map((c) => (c.id === comentario.id ? comentario : c)),
    );
    definirStatus(
      comentario.removido ? "Comentário removido." : "Comentário atualizado.",
    );
  }
  return (
    <section aria-label="Conversa" className="space-y-4">
      {carregando && !pagina && <Skeleton />}
      {erro && (
        <ErrorState
          message={erro}
          onRetry={() => void carregar(pagina?.page ?? 0).catch(() => {})}
        />
      )}
      {pagina && (
        <>
          {itens.length < pagina.total && (
            <Button
              ref={anteriores}
              variant="secondary"
              disabled={carregando}
              onClick={() => void carregar(pagina.page + 1).catch(() => {})}
            >
              {carregando ? "Carregando…" : "Carregar comentários anteriores"}
            </Button>
          )}
          {!itens.length && (
            <div className="py-4 text-center">
              <p className="font-medium">Nenhum comentário ainda.</p>
              {pagina.podeComentar && (
                <p className="mt-1 subtle">
                  Comece a conversa sobre{" "}
                  {recurso === "tasks" ? "esta tarefa" : "este projeto"}.
                </p>
              )}
            </div>
          )}
          <div className="space-y-3">
            {[...itens]
              .sort(
                (a, b) =>
                  a.criadaEm.localeCompare(b.criadaEm) ||
                  a.id.localeCompare(b.id),
              )
              .map((c) => (
                <ComentarioItem
                  key={c.id}
                  comentario={c}
                  aoAtualizar={atualizar}
                  aoRecarregar={recarregar}
                />
              ))}
          </div>
          {pagina.podeComentar ? (
            <form
              className="space-y-2 border-t border-border pt-4"
              onSubmit={(e) => {
                e.preventDefault();
                void enviar();
              }}
            >
              <label htmlFor={campo} className="text-sm font-medium">
                Escreva um comentário…
              </label>
              <Textarea
                id={campo}
                maxLength={5000}
                value={texto}
                onChange={(e) => definirTexto(e.target.value)}
                disabled={enviando}
              />
              {texto.length >= 4500 && (
                <p className="subtle text-xs">{texto.length}/5000 caracteres</p>
              )}
              <Button type="submit" disabled={enviando || !texto.trim()}>
                {enviando ? "Enviando…" : "Comentar"}
              </Button>
              {erroEnvio && <ErrorState message={erroEnvio} />}
            </form>
          ) : (
            <p className="subtle">
              Comentários disponíveis somente para leitura no seu acesso atual.
            </p>
          )}
        </>
      )}
      <p role="status" aria-live="polite" className="subtle">
        {status}
      </p>
    </section>
  );
}
