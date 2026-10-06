"use client";
import { useState } from "react";
import {
  arquivarTarefa,
  buscarTarefa,
  criarTarefa,
  editarTarefa,
  formatarPrazoTarefa,
  prioridadesTarefa,
  statusTarefa,
  mensagemConflitoTarefa,
  type DadosTarefa,
  type OpcoesTarefas,
  type Tarefa,
} from "@/lib/api/tarefas";
import { ErroApi } from "@/lib/api/http";
import { Dialog } from "../ui/dialog";
import { Button } from "../ui/button";
import { ErrorState } from "../ui/feedback";
import { FormularioTarefa } from "./formulario-tarefa";

export function DetalheTarefa({
  inicial,
  opcoes,
  podeAtribuir,
  podeVerProjetos = false,
  aoFechar,
  aoConcluir,
}: {
  inicial: Tarefa | null;
  opcoes: OpcoesTarefas;
  podeAtribuir: boolean;
  podeVerProjetos?: boolean;
  aoFechar: () => void;
  aoConcluir: (mensagem: string) => void;
}) {
  const [tarefa, definirTarefa] = useState(inicial);
  const [modo, definirModo] = useState<"ver" | "editar" | "arquivar">(
    inicial ? "ver" : "editar",
  );
  const [ocupada, definirOcupada] = useState(false);
  const [erro, definirErro] = useState("");
  const [conflito, definirConflito] = useState(false);
  async function executar(acao: () => Promise<unknown>, mensagem: string) {
    definirOcupada(true);
    definirErro("");
    try {
      await acao();
      aoConcluir(mensagem);
    } catch (erro) {
      definirErro(
        erro instanceof Error
          ? erro.message
          : "Não foi possível concluir a operação.",
      );
      definirConflito(
        erro instanceof ErroApi &&
          erro.status === 409 &&
          erro.message === mensagemConflitoTarefa,
      );
    } finally {
      definirOcupada(false);
    }
  }
  async function atualizar() {
    if (!tarefa) return;
    definirOcupada(true);
    try {
      definirTarefa(await buscarTarefa(tarefa.id));
      definirModo("ver");
      definirConflito(false);
      definirErro("");
    } catch (erro) {
      definirErro(
        erro instanceof Error ? erro.message : "Não foi possível atualizar.",
      );
    } finally {
      definirOcupada(false);
    }
  }
  async function salvar(dados: DadosTarefa) {
    await executar(
      () => (tarefa ? editarTarefa(tarefa, dados) : criarTarefa(dados)),
      tarefa ? "Tarefa atualizada." : "Tarefa criada.",
    );
  }
  return (
    <Dialog
      open
      onClose={aoFechar}
      busy={ocupada}
      title={
        !tarefa
          ? "Nova tarefa"
          : modo === "arquivar"
            ? "Arquivar tarefa?"
            : "Detalhes da tarefa"
      }
    >
      {erro && (
        <div className="mb-4">
          <ErrorState message={erro} />
          {conflito && (
            <Button
              className="mt-2"
              variant="secondary"
              disabled={ocupada}
              onClick={atualizar}
            >
              Atualizar dados
            </Button>
          )}
        </div>
      )}
      {modo === "editar" && (
        <FormularioTarefa
          key={`${tarefa?.id ?? "nova"}-${tarefa?.versao ?? 0}`}
          tarefa={tarefa ?? undefined}
          opcoes={opcoes}
          podeAtribuir={tarefa ? tarefa.capacidades.atribuir : podeAtribuir}
          podeVerProjetos={podeVerProjetos}
          ocupada={ocupada}
          bloqueada={conflito}
          aoSalvar={salvar}
        />
      )}
      {modo === "ver" && tarefa && (
        <div className="space-y-4">
          <h3 className="break-words text-xl font-semibold">{tarefa.titulo}</h3>
          <p className="whitespace-pre-wrap break-words subtle">
            {tarefa.descricao || "Sem descrição."}
          </p>
          <dl className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <dt className="subtle">Status</dt>
              <dd>
                {statusTarefa[tarefa.status]}
                {tarefa.arquivada ? " · Arquivada" : ""}
              </dd>
            </div>
            <div>
              <dt className="subtle">Prioridade</dt>
              <dd>{prioridadesTarefa[tarefa.prioridade]}</dd>
            </div>
            <div>
              <dt className="subtle">Equipe</dt>
              <dd>{tarefa.equipe.nome}</dd>
            </div>
            <div>
              <dt className="subtle">Prazo</dt>
              <dd>
                {tarefa.prazo ? formatarPrazoTarefa(tarefa.prazo) : "Sem prazo"}
                {tarefa.atrasada ? " · Atrasada" : ""}
              </dd>
            </div>
            <div>
              <dt className="subtle">Criada por</dt>
              <dd>{tarefa.criadaPor.nome}</dd>
            </div>
            <div>
              <dt className="subtle">Responsáveis</dt>
              <dd>
                {tarefa.responsaveis.map((pessoa) => pessoa.nome).join(", ") ||
                  "Sem responsáveis"}
              </dd>
            </div>
          </dl>
          {!tarefa.capacidades.editar && (
            <p className="subtle">
              Esta tarefa está disponível somente para leitura no seu acesso
              atual.
            </p>
          )}
          <div className="flex flex-wrap gap-2">
            {tarefa.capacidades.editar && (
              <Button onClick={() => definirModo("editar")}>
                Editar tarefa
              </Button>
            )}
            {tarefa.capacidades.arquivar && (
              <Button
                variant="secondary"
                onClick={() => definirModo("arquivar")}
              >
                Arquivar tarefa
              </Button>
            )}
          </div>
        </div>
      )}
      {modo === "arquivar" && tarefa && (
        <div className="space-y-4">
          <p>
            Arquivar “{tarefa.titulo}”? A tarefa sairá do quadro ativo e
            continuará disponível no histórico de arquivadas.
          </p>
          <div className="flex flex-wrap gap-2">
            <Button
              variant="danger"
              disabled={ocupada || conflito}
              onClick={() =>
                executar(() => arquivarTarefa(tarefa), "Tarefa arquivada.")
              }
            >
              {ocupada ? "Arquivando…" : "Confirmar arquivamento"}
            </Button>
            <Button
              variant="secondary"
              disabled={ocupada}
              onClick={() => definirModo("ver")}
            >
              Cancelar
            </Button>
          </div>
        </div>
      )}
    </Dialog>
  );
}
