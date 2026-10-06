"use client";
import { AbasColaboracao } from "../comentarios/abas-colaboracao";
import Link from "next/link";
import { useState } from "react";
import {
  arquivarProjeto,
  buscarProjeto,
  criarProjeto,
  editarProjeto,
  mensagemConflitoProjeto,
  statusProjeto,
  type Projeto,
  type DadosProjeto,
  type OpcoesProjetos,
} from "@/lib/api/projetos";
import { ErroApi } from "@/lib/api/http";
import { formatarPrazoTarefa } from "@/lib/api/tarefas";
import { Dialog } from "../ui/dialog";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import { ErrorState } from "../ui/feedback";
import { ProgressoProjeto } from "./card-projeto";
import { FormularioProjeto } from "./formulario-projeto";

export function DetalheProjeto({
  inicial,
  opcoes,
  podeGerenciar,
  podeVerTarefas,
  aoFechar,
  aoConcluir,
}: {
  inicial: Projeto | null;
  opcoes: OpcoesProjetos;
  podeGerenciar: boolean;
  podeVerTarefas: boolean;
  aoFechar: () => void;
  aoConcluir: (mensagem: string) => void;
}) {
  const [projeto, definirProjeto] = useState(inicial);
  const [modo, definirModo] = useState<"ver" | "editar" | "arquivar">(
    inicial ? "ver" : "editar",
  );
  const [ocupada, definirOcupada] = useState(false),
    [erro, definirErro] = useState(""),
    [conflito, definirConflito] = useState(false);
  async function executar(acao: () => Promise<unknown>, mensagem: string) {
    definirOcupada(true);
    definirErro("");
    try {
      await acao();
      aoConcluir(mensagem);
    } catch (e) {
      definirErro(
        e instanceof Error
          ? e.message
          : "Não foi possível concluir a operação.",
      );
      definirConflito(
        e instanceof ErroApi && e.message === mensagemConflitoProjeto,
      );
    } finally {
      definirOcupada(false);
    }
  }
  async function atualizar() {
    if (!projeto) return;
    definirOcupada(true);
    try {
      definirProjeto(await buscarProjeto(projeto.id));
      definirModo("ver");
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
  async function salvar(dados: DadosProjeto) {
    await executar(
      () => (projeto ? editarProjeto(projeto, dados) : criarProjeto(dados)),
      projeto
        ? dados.status !== projeto.status
          ? "Status atualizado."
          : "Projeto atualizado."
        : "Projeto criado.",
    );
  }
  return (
    <Dialog
      open
      onClose={aoFechar}
      busy={ocupada}
      title={
        !projeto
          ? "Novo projeto"
          : modo === "arquivar"
            ? "Arquivar projeto?"
            : "Detalhes do projeto"
      }
    >
      <div className="space-y-5 p-5">
        {erro && <ErrorState message={erro} />}
        {conflito && (
          <Button variant="secondary" disabled={ocupada} onClick={atualizar}>
            Atualizar dados do projeto
          </Button>
        )}
        {modo === "editar" ? (
          <FormularioProjeto
            key={projeto?.versao ?? "novo"}
            projeto={projeto ?? undefined}
            opcoes={opcoes}
            podeGerenciar={
              projeto
                ? projeto.capacidades.gerenciarResponsaveis
                : podeGerenciar
            }
            ocupada={ocupada}
            bloqueada={conflito}
            aoSalvar={salvar}
          />
        ) : modo === "arquivar" && projeto ? (
          <>
            <p className="subtle">
              O projeto será arquivado e sairá das visualizações ativas. O
              histórico e as tarefas concluídas serão preservados.
            </p>
            <Button
              disabled={ocupada || conflito}
              onClick={() =>
                executar(() => arquivarProjeto(projeto), "Projeto arquivado.")
              }
            >
              {ocupada ? "Arquivando…" : "Confirmar arquivamento"}
            </Button>
          </>
        ) : (
          projeto && (
            <AbasColaboracao recurso="projects" recursoId={projeto.id}>
              <div className="flex flex-wrap gap-2">
                <Badge
                  tone={projeto.status === "CONCLUIDO" ? "success" : "pink"}
                >
                  {statusProjeto[projeto.status]}
                </Badge>
                {projeto.arquivado && <Badge>Arquivado</Badge>}
              </div>
              <div>
                <h3 className="break-words text-xl font-semibold">
                  {projeto.titulo}
                </h3>
                <p className="mt-1 subtle">{projeto.equipe.nome}</p>
              </div>
              {projeto.descricao && (
                <p className="whitespace-pre-wrap break-words text-sm">
                  {projeto.descricao}
                </p>
              )}
              <dl className="grid gap-4 text-sm sm:grid-cols-2">
                <div>
                  <dt className="subtle">Período</dt>
                  <dd>
                    {projeto.dataInicio
                      ? formatarPrazoTarefa(projeto.dataInicio)
                      : "Sem início"}{" "}
                    —{" "}
                    {projeto.dataFim
                      ? formatarPrazoTarefa(projeto.dataFim)
                      : "Sem prazo"}
                  </dd>
                </div>
                <div>
                  <dt className="subtle">Criado por</dt>
                  <dd>{projeto.criadaPor.nome}</dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="subtle">Responsáveis</dt>
                  <dd>
                    {projeto.responsaveis.map((p) => p.nome).join(", ") ||
                      "Sem responsáveis"}
                  </dd>
                </div>
              </dl>
              <section
                aria-label="Tarefas do projeto"
                className="space-y-3 rounded-xl border border-border p-4"
              >
                <h3 className="font-medium">Tarefas relacionadas</h3>
                <ProgressoProjeto projeto={projeto} />
                {projeto.totalTarefas > 0 && (
                  <p className="subtle">
                    {projeto.aFazer} a fazer · {projeto.emAndamento} em
                    andamento · {projeto.emRevisao} em revisão ·{" "}
                    {projeto.tarefasConcluidas} concluídas
                  </p>
                )}
                {podeVerTarefas && (
                  <Link
                    className="inline-flex min-h-11 items-center text-sm font-medium text-primary"
                    href={`/tarefas?projetoId=${encodeURIComponent(projeto.id)}`}
                  >
                    Ver tarefas
                  </Link>
                )}
              </section>
              <div className="flex flex-wrap gap-3">
                {projeto.capacidades.editar && (
                  <Button
                    disabled={ocupada}
                    onClick={() => definirModo("editar")}
                  >
                    Editar projeto
                  </Button>
                )}
                {projeto.capacidades.arquivar && (
                  <Button
                    variant="secondary"
                    disabled={ocupada}
                    onClick={() => definirModo("arquivar")}
                  >
                    Arquivar projeto
                  </Button>
                )}
              </div>
              {!projeto.capacidades.editar && (
                <p className="subtle">
                  Você pode consultar este projeto. A edição não está
                  disponível.
                </p>
              )}
            </AbasColaboracao>
          )
        )}
        {projeto && modo !== "ver" && (
          <Button
            variant="ghost"
            disabled={ocupada}
            onClick={() => {
              definirModo("ver");
              definirErro("");
            }}
          >
            Voltar aos detalhes
          </Button>
        )}
      </div>
    </Dialog>
  );
}
