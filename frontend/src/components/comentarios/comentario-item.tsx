"use client";
import { useId, useRef, useState } from "react";
import { ErroApi } from "@/lib/api/http";
import {
  conflitoComentario,
  editarComentario,
  removerComentario,
  formatarDataColaboracao,
  type Comentario,
} from "@/lib/api/comentarios";
import { Button } from "../ui/button";
import { Textarea } from "../ui/textarea";
import { ErrorState } from "../ui/feedback";

export function ComentarioItem({
  comentario,
  aoAtualizar,
  aoRecarregar,
}: {
  comentario: Comentario;
  aoAtualizar: (comentario: Comentario) => void;
  aoRecarregar: () => Promise<void>;
}) {
  const id = useId(),
    artigo = useRef<HTMLElement>(null),
    botaoEditar = useRef<HTMLButtonElement>(null),
    botaoRemover = useRef<HTMLButtonElement>(null);
  function voltar() {
    const anterior = modo;
    definirModo("ver");
    definirErro("");
    definirConflito(false);
    requestAnimationFrame(() =>
      (anterior === "editar" ? botaoEditar : botaoRemover).current?.focus(),
    );
  }
  const [modo, definirModo] = useState<"ver" | "editar" | "remover">("ver");
  const [texto, definirTexto] = useState(comentario.conteudo ?? "");
  const [ocupada, definirOcupada] = useState(false),
    [erro, definirErro] = useState(""),
    [conflito, definirConflito] = useState(false);
  async function executar(remover: boolean) {
    definirOcupada(true);
    definirErro("");
    try {
      const atualizado = await (remover
        ? removerComentario(comentario)
        : editarComentario(comentario, texto));
      aoAtualizar(atualizado);
      definirModo("ver");
      definirConflito(false);
      requestAnimationFrame(() =>
        (remover ? artigo.current : botaoEditar.current)?.focus(),
      );
    } catch (e) {
      definirErro(
        e instanceof Error ? e.message : "Não foi possível concluir a ação.",
      );
      definirConflito(
        e instanceof ErroApi &&
          e.status === 409 &&
          e.message === conflitoComentario,
      );
    } finally {
      definirOcupada(false);
    }
  }
  async function atualizar() {
    definirOcupada(true);
    try {
      await aoRecarregar();
      definirConflito(false);
      definirErro("");
    } catch (e) {
      definirErro(
        e instanceof Error ? e.message : "Não foi possível atualizar.",
      );
    } finally {
      definirOcupada(false);
    }
  }
  return (
    <article
      ref={artigo}
      tabIndex={-1}
      className="space-y-3 rounded-xl border border-border p-3 text-sm"
      aria-label={`Comentário de ${comentario.autora.nome}`}
    >
      <div className="flex items-center gap-2">
        <span
          aria-hidden
          className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold"
        >
          {comentario.autora.nome
            .split(" ")
            .filter(Boolean)
            .slice(0, 2)
            .map((n) => n[0])
            .join("")}
        </span>
        <div className="min-w-0">
          <p className="break-words font-medium">{comentario.autora.nome}</p>
          <time
            className="subtle text-xs"
            dateTime={comentario.criadaEm}
            title={new Date(comentario.criadaEm).toLocaleString("pt-BR")}
          >
            {formatarDataColaboracao(comentario.criadaEm)}
          </time>
          {comentario.editado && (
            <span className="ml-2 subtle text-xs">Editado</span>
          )}
        </div>
      </div>
      {comentario.removido ? (
        <p className="subtle italic">Comentário removido.</p>
      ) : modo === "editar" ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void executar(false);
          }}
          className="space-y-2"
        >
          <label htmlFor={id} className="font-medium">
            Editar comentário
          </label>
          <Textarea
            id={id}
            autoFocus
            maxLength={5000}
            value={texto}
            onChange={(e) => definirTexto(e.target.value)}
            disabled={ocupada || conflito}
          />
          <div className="flex flex-wrap gap-2">
            <Button
              type="submit"
              disabled={ocupada || conflito || !texto.trim()}
            >
              {ocupada ? "Salvando…" : "Salvar comentário"}
            </Button>
            <Button
              type="button"
              variant="ghost"
              disabled={ocupada}
              onClick={voltar}
            >
              Cancelar
            </Button>
          </div>
        </form>
      ) : (
        <p className="whitespace-pre-wrap break-words [overflow-wrap:anywhere]">
          {comentario.conteudo}
        </p>
      )}
      {modo === "remover" && !comentario.removido && (
        <div role="group" aria-label="Confirmar remoção" className="space-y-2">
          <p className="font-medium">Remover este comentário?</p>
          <p className="subtle">Ele aparecerá como removido na conversa.</p>
          <div className="flex flex-wrap gap-2">
            <Button
              autoFocus
              variant="danger"
              disabled={ocupada || conflito}
              onClick={() => void executar(true)}
            >
              {ocupada ? "Removendo…" : "Confirmar remoção"}
            </Button>
            <Button variant="ghost" disabled={ocupada} onClick={voltar}>
              Cancelar
            </Button>
          </div>
        </div>
      )}
      {modo === "ver" && !comentario.removido && (
        <div className="flex gap-2">
          {comentario.capacidades.editar && (
            <Button
              ref={botaoEditar}
              variant="ghost"
              onClick={() => {
                definirTexto(comentario.conteudo ?? "");
                definirModo("editar");
              }}
            >
              Editar comentário
            </Button>
          )}
          {comentario.capacidades.remover && (
            <Button
              ref={botaoRemover}
              variant="ghost"
              onClick={() => definirModo("remover")}
            >
              Remover comentário
            </Button>
          )}
        </div>
      )}
      {erro && <ErrorState message={erro} />}
      {conflito && (
        <Button
          variant="secondary"
          disabled={ocupada}
          onClick={() => void atualizar()}
        >
          Atualizar comentários
        </Button>
      )}
    </article>
  );
}
