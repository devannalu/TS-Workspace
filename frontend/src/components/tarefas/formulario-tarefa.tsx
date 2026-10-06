"use client";
import { useEffect, useState, type FormEvent } from "react";
import {
  buscarOpcoesTarefas,
  prioridadesTarefa,
  type DadosTarefa,
  type OpcoesTarefas,
  type PrioridadeTarefa,
  type Tarefa,
} from "@/lib/api/tarefas";
import { Input } from "../ui/input";
import { Button } from "../ui/button";
import { ErrorState } from "../ui/feedback";

export function FormularioTarefa({
  tarefa,
  opcoes,
  podeAtribuir,
  ocupada,
  bloqueada = false,
  aoSalvar,
}: {
  tarefa?: Tarefa;
  opcoes: OpcoesTarefas;
  podeAtribuir: boolean;
  ocupada: boolean;
  bloqueada?: boolean;
  aoSalvar: (dados: DadosTarefa) => Promise<void>;
}) {
  const [equipeId, definirEquipe] = useState(tarefa?.equipe.id ?? "");
  const [responsaveis, definirResponsaveis] = useState(
    tarefa?.responsaveis.map((pessoa) => pessoa.id) ?? [],
  );
  const [integrantes, definirIntegrantes] = useState<
    OpcoesTarefas["responsaveis"]
  >([]);
  const [erro, definirErro] = useState("");
  const [carregando, definirCarregando] = useState(false);
  useEffect(() => {
    if (!equipeId) return;
    let atual = true;
    buscarOpcoesTarefas(equipeId)
      .then((dados) => {
        if (atual) {
          definirIntegrantes(dados.responsaveis);
          definirErro("");
          definirCarregando(false);
        }
      })
      .catch((erro) => {
        if (atual) {
          definirErro(erro.message);
          definirCarregando(false);
        }
      });
    return () => {
      atual = false;
    };
  }, [equipeId]);
  async function salvar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const dados = new FormData(evento.currentTarget);
    await aoSalvar({
      titulo: String(dados.get("titulo")).trim(),
      descricao: String(dados.get("descricao")).trim() || null,
      prioridade: dados.get("prioridade") as PrioridadeTarefa,
      equipeId,
      prazo: String(dados.get("prazo")) || null,
      ...(podeAtribuir ? { responsavelIds: responsaveis } : {}),
    });
  }
  return (
    <form onSubmit={salvar} className="space-y-4">
      <fieldset disabled={ocupada || bloqueada} className="space-y-4">
        <label className="block space-y-1">
          <span>Título</span>
          <Input
            name="titulo"
            required
            maxLength={200}
            defaultValue={tarefa?.titulo}
            autoFocus
          />
        </label>
        <label className="block space-y-1">
          <span>Descrição</span>
          <textarea
            name="descricao"
            maxLength={5000}
            defaultValue={tarefa?.descricao ?? ""}
            rows={4}
            className="task-select w-full resize-y"
          />
        </label>
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block space-y-1">
            <span>Equipe</span>
            <select
              required
              className="task-select w-full"
              value={equipeId}
              onChange={(evento) => {
                definirEquipe(evento.target.value);
                definirIntegrantes([]);
                definirResponsaveis([]);
                definirCarregando(!!evento.target.value);
              }}
              disabled={
                !!tarefa && !podeAtribuir && tarefa.responsaveis.length > 0
              }
            >
              <option value="">Selecione uma equipe</option>
              {opcoes.equipes.map((equipe) => (
                <option key={equipe.id} value={equipe.id}>
                  {equipe.nome}
                </option>
              ))}
            </select>
          </label>
          <label className="block space-y-1">
            <span>Prioridade</span>
            <select
              name="prioridade"
              className="task-select w-full"
              defaultValue={tarefa?.prioridade ?? "MEDIA"}
            >
              {Object.entries(prioridadesTarefa).map(([valor, texto]) => (
                <option key={valor} value={valor}>
                  {texto}
                </option>
              ))}
            </select>
          </label>
        </div>
        <label className="block space-y-1">
          <span>Prazo</span>
          <Input name="prazo" type="date" defaultValue={tarefa?.prazo ?? ""} />
        </label>
        {podeAtribuir && (
          <fieldset className="space-y-2 rounded-xl border border-border p-3">
            <legend className="px-1 text-sm font-medium">Responsáveis</legend>
            <p className="subtle">
              Opcional. Somente integrantes ativos da equipe. Ao trocar a
              equipe, selecione novamente.
            </p>
            {carregando ? (
              <p role="status">Carregando integrantes…</p>
            ) : integrantes.length ? (
              integrantes.map((pessoa) => (
                <label
                  key={pessoa.id}
                  className="flex min-h-11 items-center gap-3 text-sm"
                >
                  <input
                    type="checkbox"
                    checked={responsaveis.includes(pessoa.id)}
                    onChange={(evento) =>
                      definirResponsaveis((atuais) =>
                        evento.target.checked
                          ? [...atuais, pessoa.id]
                          : atuais.filter((id) => id !== pessoa.id),
                      )
                    }
                  />
                  {pessoa.nome}
                </label>
              ))
            ) : (
              <p className="subtle">
                {equipeId
                  ? "Nenhuma integrante disponível."
                  : "Selecione uma equipe."}
              </p>
            )}
          </fieldset>
        )}
      </fieldset>
      {erro && <ErrorState message={erro} />}
      <Button type="submit" disabled={ocupada || bloqueada || carregando || !!erro}>
        {ocupada ? "Salvando…" : tarefa ? "Salvar alterações" : "Criar tarefa"}
      </Button>
    </form>
  );
}
