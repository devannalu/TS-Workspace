"use client";
import { useCallback, useEffect, useState } from "react";
import { Plus } from "lucide-react";
import {
  buscarProjeto,
  buscarOpcoesProjetos,
  listarProjetos,
  statusProjeto,
  type Projeto,
  type OpcoesProjetos,
  type FiltrosProjetos,
} from "@/lib/api/projetos";
import type { PaginaApi } from "@/lib/api/contratos";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Pagination } from "../ui/pagination";
import { EmptyState, ErrorState, Toast } from "../ui/feedback";
import { CardProjeto } from "./card-projeto";
import { DetalheProjeto } from "./detalhe-projeto";

export function ListaProjetos({
  usuarioId,
  permissoes,
  opcoes,
}: {
  usuarioId: string;
  permissoes: string[];
  opcoes: OpcoesProjetos;
}) {
  const [filtros, definirFiltros] = useState<FiltrosProjetos>({}),
    [pagina, definirPagina] = useState(0),
    [atualizacao, definirAtualizacao] = useState(0);
  const [dados, definirDados] = useState<PaginaApi<Projeto>>(),
    [carregando, definirCarregando] = useState(true),
    [erro, definirErro] = useState("");
  const [integrantes, definirIntegrantes] = useState<
      OpcoesProjetos["responsaveis"]
    >([]),
    [detalhe, definirDetalhe] = useState<Projeto | null>(),
    [abrindo, definirAbrindo] = useState(false),
    [mensagem, definirMensagem] = useState("");
  useEffect(() => {
    let atual = true;
    const temporizador = setTimeout(() => {
      listarProjetos(filtros, pagina)
        .then((d) => {
          if (atual) {
            definirDados(d);
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
    }, 250);
    return () => {
      atual = false;
      clearTimeout(temporizador);
    };
  }, [filtros, pagina, atualizacao]);
  useEffect(() => {
    if (!filtros.teamId) return;
    let atual = true;
    buscarOpcoesProjetos(filtros.teamId)
      .then((d) => {
        if (atual) definirIntegrantes(d.responsaveis);
      })
      .catch((e) => {
        if (atual) definirErro(e.message);
      });
    return () => {
      atual = false;
    };
  }, [filtros.teamId]);
  function filtrar(novos: FiltrosProjetos) {
    definirFiltros(novos);
    definirPagina(0);
    definirCarregando(true);
  }
  async function abrir(id: string) {
    definirAbrindo(true);
    try {
      definirDetalhe(await buscarProjeto(id));
    } catch (e) {
      definirErro(
        e instanceof Error ? e.message : "Não foi possível abrir o projeto.",
      );
    } finally {
      definirAbrindo(false);
    }
  }
  const fecharToast = useCallback(() => definirMensagem(""), []);
  const temFiltro = Object.values(filtros).some(Boolean);
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="page-title">Projetos</h1>
          <p className="mt-2 subtle">
            Objetivos compartilhados, um passo de cada vez.
          </p>
        </div>
        {permissoes.includes("projects.create") && (
          <Button onClick={() => definirDetalhe(null)}>
            <Plus size={17} aria-hidden />
            Novo projeto
          </Button>
        )}
      </div>
      <section
        aria-label="Filtros de projetos"
        className="space-y-3 rounded-2xl border border-border bg-card p-4"
      >
        <label className="block space-y-1 text-sm">
          <span>Buscar projeto</span>
          <Input
            value={filtros.search ?? ""}
            maxLength={200}
            placeholder="Título ou descrição"
            onChange={(e) => filtrar({ ...filtros, search: e.target.value })}
          />
        </label>
        <details className="group">
          <summary className="flex min-h-11 cursor-pointer items-center text-sm font-medium">
            Filtros
          </summary>
          <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            <label className="space-y-1 text-sm">
              <span>Equipe</span>
              <select
                className="task-select w-full"
                value={filtros.teamId ?? ""}
                onChange={(e) => {
                  definirIntegrantes([]);
                  filtrar({
                    ...filtros,
                    teamId: e.target.value,
                    responsibleId: undefined,
                  });
                }}
              >
                <option value="">Todas as equipes</option>
                {opcoes.equipes.map((e) => (
                  <option key={e.id} value={e.id}>
                    {e.nome}
                  </option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-sm">
              <span>Status</span>
              <select
                className="task-select w-full"
                value={filtros.status ?? ""}
                onChange={(e) =>
                  filtrar({
                    ...filtros,
                    status: e.target.value as FiltrosProjetos["status"],
                  })
                }
              >
                <option value="">Todos os status</option>
                {Object.entries(statusProjeto).map(([valor, texto]) => (
                  <option key={valor} value={valor}>
                    {texto}
                  </option>
                ))}
              </select>
            </label>
            <label className="space-y-1 text-sm">
              <span>Responsável</span>
              <select
                className="task-select w-full"
                value={filtros.responsibleId ?? ""}
                onChange={(e) =>
                  filtrar({ ...filtros, responsibleId: e.target.value })
                }
              >
                <option value="">Todas as responsáveis</option>
                <option value={usuarioId}>Eu</option>
                {integrantes
                  .filter((pessoa) => pessoa.id !== usuarioId)
                  .map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nome}
                    </option>
                  ))}
              </select>
            </label>
          </div>
        </details>
        <div className="flex flex-wrap items-center gap-3">
          <Button
            variant="secondary"
            aria-pressed={filtros.responsibleId === usuarioId}
            onClick={() =>
              filtrar({
                ...filtros,
                responsibleId:
                  filtros.responsibleId === usuarioId ? undefined : usuarioId,
              })
            }
          >
            Sou responsável
          </Button>
          <Button
            variant="ghost"
            onClick={() => {
              definirIntegrantes([]);
              filtrar({});
            }}
          >
            Limpar filtros
          </Button>
          <label className="flex min-h-11 items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={!!filtros.archived}
              onChange={(e) =>
                filtrar({ ...filtros, archived: e.target.checked })
              }
            />
            Arquivados
          </label>
        </div>
      </section>
      {erro && (
        <ErrorState
          message={erro}
          onRetry={() => {
            definirCarregando(true);
            definirAtualizacao((v) => v + 1);
          }}
        />
      )}
      {abrindo && (
        <p role="status" className="subtle">
          Carregando detalhes…
        </p>
      )}
      {carregando ? (
        <div
          role="status"
          aria-label="Carregando projetos"
          className="grid gap-4 md:grid-cols-2 xl:grid-cols-3"
        >
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-64 animate-pulse rounded-2xl bg-muted" />
          ))}
        </div>
      ) : !erro && dados?.items.length ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {dados.items.map((p) => (
              <CardProjeto
                key={p.id}
                projeto={p}
                aoAbrir={() => {
                  if (!abrindo) void abrir(p.id);
                }}
              />
            ))}
          </div>
          <Pagination
            page={dados.page}
            size={dados.size}
            total={dados.total}
            label="projetos"
            onPage={(p) => {
              definirPagina(p);
              definirCarregando(true);
            }}
          />
        </>
      ) : (
        !erro && (
          <EmptyState
            title={
              filtros.responsibleId === usuarioId
                ? "Você ainda não é responsável por um projeto"
                : temFiltro
                  ? "Não encontramos projetos"
                  : "Nenhum projeto por aqui ainda"
            }
            description={
              temFiltro
                ? "Ajuste os filtros para encontrar outros projetos."
                : permissoes.includes("projects.create")
                  ? "Crie um projeto para organizar o próximo objetivo da equipe."
                  : "Os projetos da sua equipe aparecerão aqui."
            }
          />
        )
      )}
      {detalhe !== undefined && (
        <DetalheProjeto
          inicial={detalhe}
          opcoes={opcoes}
          podeGerenciar={permissoes.includes("projects.manage_members")}
          podeVerTarefas={permissoes.includes("tasks.view")}
          aoFechar={() => definirDetalhe(undefined)}
          aoConcluir={(texto) => {
            definirDetalhe(undefined);
            definirMensagem(texto);
            definirAtualizacao((v) => v + 1);
          }}
        />
      )}
      <Toast message={mensagem} onClose={fecharToast} />
    </div>
  );
}
