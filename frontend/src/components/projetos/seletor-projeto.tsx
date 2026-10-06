"use client";
import { useEffect, useState } from "react";
import { listarProjetos, type Projeto } from "@/lib/api/projetos";
import type { ReferenciaTarefa } from "@/lib/api/tarefas";
import { Input } from "../ui/input";
import { Button } from "../ui/button";
import { ErrorState } from "../ui/feedback";

export function SeletorProjeto({
  equipeId,
  valor,
  atual,
  somenteElegiveis = false,
  aoSelecionar,
}: {
  equipeId?: string;
  valor: string;
  atual?: ReferenciaTarefa | null;
  somenteElegiveis?: boolean;
  aoSelecionar: (id: string) => void;
}) {
  const [busca, definirBusca] = useState(""),
    [pagina, definirPagina] = useState(0),
    [projetos, definirProjetos] = useState<Projeto[]>([]);
  const [temMais, definirTemMais] = useState(false),
    [carregando, definirCarregando] = useState(true),
    [erro, definirErro] = useState("");
  useEffect(() => {
    if (somenteElegiveis && !equipeId) return;
    let ativa = true;
    const tempo = setTimeout(() => {
      listarProjetos({ teamId: equipeId, search: busca }, pagina)
        .then((d) => {
          if (ativa) {
            definirProjetos((anteriores) =>
              pagina ? [...anteriores, ...d.items] : d.items,
            );
            definirTemMais((d.page + 1) * d.size < d.total);
            definirErro("");
            definirCarregando(false);
          }
        })
        .catch((e) => {
          if (ativa) {
            definirErro(e.message);
            definirCarregando(false);
          }
        });
    }, 250);
    return () => {
      ativa = false;
      clearTimeout(tempo);
    };
  }, [equipeId, busca, pagina, somenteElegiveis]);
  const elegiveis = projetos.filter(
    (p) => !somenteElegiveis || p.status !== "CONCLUIDO",
  );
  return (
    <div className="space-y-2">
      <label className="block space-y-1 text-sm">
        <span>Buscar projeto na lista</span>
        <Input
          value={busca}
          disabled={somenteElegiveis && !equipeId}
          maxLength={200}
          placeholder="Buscar pelo título"
          onChange={(e) => {
            definirBusca(e.target.value);
            definirPagina(0);
            definirCarregando(true);
          }}
        />
      </label>
      <label className="block space-y-1 text-sm">
        <span>Projeto</span>
        <select
          className="task-select w-full"
          value={valor}
          disabled={somenteElegiveis && !equipeId}
          onChange={(e) => aoSelecionar(e.target.value)}
        >
          <option value="">
            {somenteElegiveis ? "Sem projeto" : "Todos os projetos"}
          </option>
          {valor && !elegiveis.some((p) => p.id === valor) && (
            <option value={valor}>
              {atual?.nome ?? "Projeto selecionado"}
            </option>
          )}
          {elegiveis.map((p) => (
            <option key={p.id} value={p.id}>
              {p.titulo}
            </option>
          ))}
        </select>
      </label>
      {carregando && (!somenteElegiveis || equipeId) && (
        <p role="status" className="subtle">
          Carregando projetos…
        </p>
      )}
      {erro && <ErrorState message={erro} />}
      {temMais && (
        <Button
          type="button"
          variant="ghost"
          disabled={carregando}
          onClick={() => {
            definirPagina((p) => p + 1);
            definirCarregando(true);
          }}
        >
          Carregar mais projetos
        </Button>
      )}
    </div>
  );
}
