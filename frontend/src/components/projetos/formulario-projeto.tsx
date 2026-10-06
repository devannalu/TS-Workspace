"use client";
import { useEffect, useState, type FormEvent } from "react";
import {
  buscarOpcoesProjetos,
  statusProjeto,
  type Projeto,
  type DadosProjeto,
  type OpcoesProjetos,
  type StatusProjeto,
} from "@/lib/api/projetos";
import { Input } from "../ui/input";
import { Button } from "../ui/button";
import { ErrorState } from "../ui/feedback";

export function FormularioProjeto({
  projeto,
  opcoes,
  podeGerenciar,
  ocupada,
  bloqueada,
  aoSalvar,
}: {
  projeto?: Projeto;
  opcoes: OpcoesProjetos;
  podeGerenciar: boolean;
  ocupada: boolean;
  bloqueada: boolean;
  aoSalvar: (dados: DadosProjeto) => Promise<void>;
}) {
  const [equipeId, definirEquipe] = useState(projeto?.equipe.id ?? "");
  const [responsaveis, definirResponsaveis] = useState(
    projeto?.responsaveis.map((p) => p.id) ?? [],
  );
  const [integrantes, definirIntegrantes] = useState<
    OpcoesProjetos["responsaveis"]
  >([]);
  const [erro, definirErro] = useState("");
  const [carregando, definirCarregando] = useState(!!projeto?.equipe.id);
  useEffect(() => {
    if (!equipeId) return;
    let atual = true;
    buscarOpcoesProjetos(equipeId)
      .then((d) => {
        if (atual) {
          definirIntegrantes(d.responsaveis);
          definirErro("");
          definirCarregando(false);
        }
      })
      .catch((e) => {
        if (atual) {
          definirErro(e.message);
          definirCarregando(false);
        }
      });
    return () => {
      atual = false;
    };
  }, [equipeId]);
  async function salvar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const d = new FormData(evento.currentTarget);
    const inicio = String(d.get("dataInicio")) || null,
      fim = String(d.get("dataFim")) || null;
    if (inicio && fim && inicio > fim) {
      definirErro("A data final deve ser igual ou posterior à data de início.");
      return;
    }
    await aoSalvar({
      titulo: String(d.get("titulo")).trim(),
      descricao: String(d.get("descricao")).trim() || null,
      equipeId,
      dataInicio: inicio,
      dataFim: fim,
      ...(projeto ? { status: d.get("status") as StatusProjeto } : {}),
      ...(podeGerenciar ? { responsavelIds: responsaveis } : {}),
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
            defaultValue={projeto?.titulo}
            autoFocus
          />
        </label>
        <label className="block space-y-1">
          <span>Descrição</span>
          <textarea
            name="descricao"
            maxLength={5000}
            rows={3}
            defaultValue={projeto?.descricao ?? ""}
            className="task-select w-full resize-y"
          />
        </label>
        <label className="block space-y-1">
          <span>Equipe</span>
          <select
            required
            className="task-select w-full"
            value={equipeId}
            disabled={
              (!!projeto && !projeto.capacidades.trocarEquipe) ||
              (!!projeto && !podeGerenciar && projeto.responsaveis.length > 0)
            }
            onChange={(e) => {
              definirEquipe(e.target.value);
              definirIntegrantes([]);
              definirCarregando(!!e.target.value);
            }}
          >
            <option value="">Selecione uma equipe</option>
            {opcoes.equipes.map((e) => (
              <option key={e.id} value={e.id}>
                {e.nome}
              </option>
            ))}
          </select>
        </label>
        {projeto && !projeto.capacidades.trocarEquipe && (
          <p className="subtle">
            A equipe é preservada porque este projeto já teve tarefas
            vinculadas.
          </p>
        )}
        {projeto && (
          <label className="block space-y-1">
            <span>Status</span>
            <select
              name="status"
              defaultValue={projeto.status}
              className="task-select w-full"
            >
              {Object.entries(statusProjeto).map(([valor, texto]) => (
                <option key={valor} value={valor}>
                  {texto}
                </option>
              ))}
            </select>
          </label>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="block space-y-1">
            <span>Data de início</span>
            <Input
              name="dataInicio"
              type="date"
              defaultValue={projeto?.dataInicio ?? ""}
            />
          </label>
          <label className="block space-y-1">
            <span>Data final</span>
            <Input
              name="dataFim"
              type="date"
              defaultValue={projeto?.dataFim ?? ""}
            />
          </label>
        </div>
        {podeGerenciar && (
          <fieldset className="space-y-2 rounded-xl border border-border p-3">
            <legend className="px-1 text-sm font-medium">Responsáveis</legend>
            <p className="subtle">
              Opcional. Ao trocar a equipe, revise as responsáveis selecionadas.
            </p>
            {responsaveis
              .filter((id) => !integrantes.some((p) => p.id === id))
              .map((id) => {
                const pessoa = projeto?.responsaveis.find((p) => p.id === id);
                return (
                  <label
                    key={id}
                    className="flex min-h-11 items-center gap-3 text-sm"
                  >
                    <input
                      type="checkbox"
                      checked
                      onChange={() =>
                        definirResponsaveis((atuais) =>
                          atuais.filter((p) => p !== id),
                        )
                      }
                    />
                    {pessoa?.nome ?? "Responsável anterior"} · Revise a equipe
                  </label>
                );
              })}
            {carregando ? (
              <p role="status">Carregando integrantes…</p>
            ) : (
              integrantes.map((p) => (
                <label
                  key={p.id}
                  className="flex min-h-11 items-center gap-3 text-sm"
                >
                  <input
                    type="checkbox"
                    checked={responsaveis.includes(p.id)}
                    onChange={(e) =>
                      definirResponsaveis((atuais) =>
                        e.target.checked
                          ? [...atuais, p.id]
                          : atuais.filter((id) => id !== p.id),
                      )
                    }
                  />
                  {p.nome}
                </label>
              ))
            )}
            {!integrantes.length && !carregando && (
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
      <Button type="submit" disabled={ocupada || bloqueada || carregando}>
        {ocupada
          ? "Salvando…"
          : projeto
            ? "Salvar alterações"
            : "Criar projeto"}
      </Button>
    </form>
  );
}
