"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  closestCenter,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { Plus } from "lucide-react";
import {
  buscarOpcoesTarefas,
  buscarTarefa,
  listarTarefas,
  moverTarefa,
  prioridadesTarefa,
  statusTarefa,
  type FiltrosTarefas,
  type OpcoesTarefas,
  type StatusTarefa,
  type Tarefa,
} from "@/lib/api/tarefas";
import type { PaginaApi } from "@/lib/api/contratos";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { EmptyState, ErrorState, Skeleton, Toast } from "../ui/feedback";
import { CardTarefa, ConteudoCardTarefa } from "./card-tarefa";
import { DetalheTarefa } from "./detalhe-tarefa";
import { SeletorProjeto } from "../projetos/seletor-projeto";

const estados = Object.keys(statusTarefa) as StatusTarefa[];
type Colunas = Partial<Record<StatusTarefa, PaginaApi<Tarefa>>>;
function ColunaTarefas({
  status,
  pagina,
  visivel,
  ocupada,
  arrasteAtivo,
  aoAbrir,
  aoMover,
  aoCarregar,
}: {
  status: StatusTarefa;
  pagina?: PaginaApi<Tarefa>;
  visivel: boolean;
  ocupada: boolean;
  arrasteAtivo: boolean;
  aoAbrir: (tarefa: Tarefa) => void;
  aoMover: (tarefa: Tarefa, status: StatusTarefa) => void;
  aoCarregar: () => void;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: status });
  return (
    <section
      ref={setNodeRef}
      aria-label={statusTarefa[status]}
      className={`task-column column-${status.toLowerCase()} ${visivel ? "mobile-visible" : ""} ${isOver ? "ring-2 ring-primary" : ""}`}
    >
      <h2 className="mb-4 flex justify-between text-sm font-semibold">
        {statusTarefa[status]}
        <span aria-label={`${pagina?.total ?? 0} tarefas`}>
          {pagina?.total ?? 0}
        </span>
      </h2>
      <SortableContext
        items={pagina?.items.map((tarefa) => tarefa.id) ?? []}
        strategy={verticalListSortingStrategy}
      >
        <div className="space-y-3">
          {pagina?.items.map((tarefa) => (
            <CardTarefa
              key={tarefa.id}
              tarefa={tarefa}
              arrasteAtivo={arrasteAtivo}
              ocupada={ocupada}
              aoAbrir={aoAbrir}
              aoMover={aoMover}
            />
          ))}
          {!pagina?.items.length && (
            <p className="rounded-xl border border-dashed border-border p-5 text-center text-xs text-muted-foreground">
              Nenhuma tarefa nesta coluna.
            </p>
          )}
        </div>
      </SortableContext>
      {pagina && pagina.items.length < pagina.total && (
        <Button
          variant="ghost"
          className="mt-3 w-full"
          disabled={ocupada}
          onClick={aoCarregar}
        >
          Carregar mais
        </Button>
      )}
    </section>
  );
}

export function QuadroTarefas({
  abrirInicial,
  usuarioId,
  permissoes,
  opcoesIniciais,
  minhasInicial = false,
  projetoInicial,
}: {
  abrirInicial?: string;
  usuarioId: string;
  permissoes: string[];
  opcoesIniciais: OpcoesTarefas;
  minhasInicial?: boolean;
  projetoInicial?: string;
}) {
  const [filtros, definirFiltros] = useState<FiltrosTarefas>({
    ...(minhasInicial ? { assigneeId: usuarioId } : {}),
    ...(projetoInicial ? { projectId: projetoInicial } : {}),
  });
  const [colunas, definirColunas] = useState<Colunas>({});
  const [opcoes, definirOpcoes] = useState(opcoesIniciais);
  const [carregando, definirCarregando] = useState(true);
  const [ocupada, definirOcupada] = useState(false);
  const [erro, definirErro] = useState("");
  const [mensagem, definirMensagem] = useState("");
  const [detalhe, definirDetalhe] = useState<Tarefa | null | undefined>(
    undefined,
  );
  const [colunaMobile, definirColunaMobile] = useState<StatusTarefa>("A_FAZER");
  const [arrasteAtivo, definirArraste] = useState(false);
  const [arrastada, definirArrastada] = useState<Tarefa | null>(null);
  const consultaAtual = useRef(0);
  const sensores = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 8 } }),
    useSensor(TouchSensor, {
      activationConstraint: { delay: 250, tolerance: 5 },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );
  const carregar = useCallback(
    async (mostrarCarregamento = true) => {
      const consulta = ++consultaAtual.current;
      if (mostrarCarregamento) definirCarregando(true);
      definirErro("");
      try {
        const visiveis = filtros.status ? [filtros.status] : estados;
        const paginas = await Promise.all(
          visiveis.map(
            async (status) =>
              [status, await listarTarefas({ ...filtros, status })] as const,
          ),
        );
        if (consulta === consultaAtual.current)
          definirColunas(Object.fromEntries(paginas));
      } catch (erro) {
        if (consulta === consultaAtual.current)
          definirErro(
            erro instanceof Error
              ? erro.message
              : "Não foi possível carregar tarefas.",
          );
      } finally {
        if (consulta === consultaAtual.current) definirCarregando(false);
      }
    },
    [filtros],
  );
  useEffect(() => {
    if (!abrirInicial || !/^[0-9a-f-]{36}$/i.test(abrirInicial)) return;
    let ativa = true;
    buscarTarefa(abrirInicial).then(recurso => { if (ativa) definirDetalhe(recurso); })
      .catch(e => { if (ativa) definirErro(e instanceof Error ? e.message : "Não foi possível abrir o recurso."); });
    return () => { ativa = false; };
  }, [abrirInicial]);
  useEffect(() => {
    // Agrupar digitação e filtros evita consultas intermediárias a cada tecla.
    const intervalo = setTimeout(() => {
      void carregar();
    }, 300);
    return () => clearTimeout(intervalo);
  }, [carregar]);
  useEffect(() => {
    const consulta = window.matchMedia("(min-width: 640px)");
    const atualizar = () => definirArraste(consulta.matches);
    atualizar();
    consulta.addEventListener("change", atualizar);
    return () => consulta.removeEventListener("change", atualizar);
  }, []);
  useEffect(() => {
    if (!filtros.teamId) return;
    let atual = true;
    buscarOpcoesTarefas(filtros.teamId)
      .then((dados) => {
        if (atual) definirOpcoes(dados);
      })
      .catch((erro) => {
        if (atual) definirErro(erro.message);
      });
    return () => {
      atual = false;
    };
  }, [filtros.teamId]);
  function filtrar(chave: keyof FiltrosTarefas, valor: string | boolean) {
    definirFiltros((atuais) => ({
      ...atuais,
      [chave]: valor || undefined,
      ...(chave === "teamId" ? { assigneeId: undefined } : {}),
    }));
  }
  async function abrir(tarefa: Tarefa) {
    definirOcupada(true);
    try {
      definirDetalhe(await buscarTarefa(tarefa.id));
    } catch (erro) {
      definirErro(
        erro instanceof Error
          ? erro.message
          : "Não foi possível abrir a tarefa.",
      );
    } finally {
      definirOcupada(false);
    }
  }
  async function mover(
    tarefa: Tarefa,
    status: StatusTarefa,
    antesDeId?: string,
  ) {
    const focoAnterior = document.activeElement;
    definirOcupada(true);
    definirErro("");
    try {
      await moverTarefa(tarefa, status, antesDeId);
      if (!arrasteAtivo) definirColunaMobile(status);
      definirMensagem(`Tarefa movida para ${statusTarefa[status]}.`);
      await carregar(false);
    } catch (erro) {
      definirErro(
        erro instanceof Error
          ? erro.message
          : "Não foi possível mover a tarefa.",
      );
    } finally {
      definirOcupada(false);
      // A atualização mantém o cartão montado e devolve foco ao controle usado.
      requestAnimationFrame(() => {
        if (!(focoAnterior instanceof HTMLElement)) return;
        const controle = focoAnterior.isConnected
          ? focoAnterior
          : document.getElementById(focoAnterior.id);
        controle?.focus();
      });
    }
  }
  function terminarArraste(evento: DragEndEvent) {
    definirArrastada(null);
    if (!evento.over || evento.active.id === evento.over.id) return;
    const tarefas = Object.values(colunas).flatMap(
      (pagina) => pagina?.items ?? [],
    );
    const tarefa = tarefas.find((tarefa) => tarefa.id === evento.active.id);
    const destino = tarefas.find((tarefa) => tarefa.id === evento.over?.id);
    const status =
      destino?.status ??
      (estados.includes(evento.over.id as StatusTarefa)
        ? (evento.over.id as StatusTarefa)
        : undefined);
    if (tarefa && status) void mover(tarefa, status, destino?.id);
  }
  async function carregarMais(status: StatusTarefa) {
    const atual = colunas[status];
    if (!atual) return;
    const consulta = consultaAtual.current;
    definirOcupada(true);
    try {
      const proxima = await listarTarefas(
        { ...filtros, status },
        atual.page + 1,
      );
      if (consulta === consultaAtual.current)
        definirColunas((colunas) => ({
          ...colunas,
          [status]: { ...proxima, items: [...atual.items, ...proxima.items] },
        }));
    } catch (erro) {
      definirErro(
        erro instanceof Error
          ? erro.message
          : "Não foi possível carregar mais tarefas.",
      );
    } finally {
      definirOcupada(false);
    }
  }
  const total = Object.values(colunas).reduce(
    (total, pagina) => total + (pagina?.total ?? 0),
    0,
  );
  return (
    <div className="space-y-5">
      <header className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="page-title">Tarefas</h1>
          <p className="mt-2 subtle">
            Organize o trabalho e acompanhe cada etapa com sua equipe.
          </p>
        </div>
        {permissoes.includes("tasks.create") && (
          <Button disabled={ocupada} onClick={() => definirDetalhe(null)}>
            <Plus size={18} aria-hidden />
            Nova tarefa
          </Button>
        )}
      </header>
      <form
        className="task-filters"
        aria-label="Filtros de tarefas"
        onSubmit={(evento) => {
          evento.preventDefault();
          void carregar();
        }}
      >
        <label>
          Buscar
          <Input
            type="search"
            maxLength={200}
            placeholder="Título ou descrição"
            value={filtros.search ?? ""}
            onChange={(evento) => filtrar("search", evento.target.value)}
          />
        </label>
        <label>
          Equipe
          <select
            className="task-select"
            value={filtros.teamId ?? ""}
            onChange={(evento) => filtrar("teamId", evento.target.value)}
          >
            <option value="">Todas as equipes</option>
            {opcoesIniciais.equipes.map((equipe) => (
              <option key={equipe.id} value={equipe.id}>
                {equipe.nome}
              </option>
            ))}
          </select>
        </label>
        <label>
          Responsável
          <select
            className="task-select"
            value={filtros.assigneeId ?? ""}
            onChange={(evento) => filtrar("assigneeId", evento.target.value)}
          >
            <option value="">Todas</option>
            <option value={usuarioId}>Eu</option>
            {filtros.teamId &&
              opcoes.responsaveis
                .filter((pessoa) => pessoa.id !== usuarioId)
                .map((pessoa) => (
                  <option key={pessoa.id} value={pessoa.id}>
                    {pessoa.nome}
                  </option>
                ))}
          </select>
        </label>
        {permissoes.includes("projects.view") && (
          <SeletorProjeto
            key={filtros.teamId ?? "todas"}
            equipeId={filtros.teamId}
            valor={filtros.projectId ?? ""}
            aoSelecionar={(id) => filtrar("projectId", id)}
          />
        )}
        <label>
          Prioridade
          <select
            className="task-select"
            value={filtros.priority ?? ""}
            onChange={(evento) => filtrar("priority", evento.target.value)}
          >
            <option value="">Todas</option>
            {Object.entries(prioridadesTarefa).map(([valor, texto]) => (
              <option key={valor} value={valor}>
                {texto}
              </option>
            ))}
          </select>
        </label>
        <label>
          Status
          <select
            className="task-select"
            value={filtros.status ?? ""}
            onChange={(evento) => {
              filtrar("status", evento.target.value);
              if (evento.target.value)
                definirColunaMobile(evento.target.value as StatusTarefa);
            }}
          >
            <option value="">Todos</option>
            {Object.entries(statusTarefa).map(([valor, texto]) => (
              <option key={valor} value={valor}>
                {texto}
              </option>
            ))}
          </select>
        </label>
        <label>
          Prazo de
          <Input
            type="date"
            value={filtros.dueFrom ?? ""}
            onChange={(evento) => filtrar("dueFrom", evento.target.value)}
          />
        </label>
        <label>
          Prazo até
          <Input
            type="date"
            value={filtros.dueTo ?? ""}
            onChange={(evento) => filtrar("dueTo", evento.target.value)}
          />
        </label>
        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="secondary"
            aria-pressed={filtros.assigneeId === usuarioId}
            onClick={() =>
              filtrar(
                "assigneeId",
                filtros.assigneeId === usuarioId ? "" : usuarioId,
              )
            }
          >
            Minhas tarefas
          </Button>
          <Button variant="ghost" onClick={() => definirFiltros({})}>
            Limpar filtros
          </Button>
        </div>
        <label className="flex min-h-11 items-center gap-2">
          <input
            type="checkbox"
            checked={filtros.archived ?? false}
            onChange={(evento) => filtrar("archived", evento.target.checked)}
          />
          Mostrar arquivadas
        </label>
      </form>
      {erro && <ErrorState message={erro} onRetry={() => void carregar()} />}
      {carregando ? (
        <Skeleton />
      ) : (
        <>
          {!total && (
            <EmptyState
              title="Nenhuma tarefa encontrada"
              description="Crie uma tarefa ou ajuste os filtros para começar."
            />
          )}
          <div
            className="task-segments"
            role="group"
            aria-label="Coluna de tarefas no celular"
          >
            {estados
              .filter((status) => !filtros.status || filtros.status === status)
              .map((status) => (
                <button
                  key={status}
                  type="button"
                  aria-pressed={colunaMobile === status}
                  onClick={() => definirColunaMobile(status)}
                >
                  {statusTarefa[status]}
                </button>
              ))}
          </div>
          <p className="sr-only">
            No arraste, pressione espaço para pegar, setas para mover, espaço
            para soltar e Escape para cancelar. Ou use Mover para no cartão.
          </p>
          <DndContext
            sensors={sensores}
            collisionDetection={closestCenter}
            onDragStart={(evento) =>
              definirArrastada(
                Object.values(colunas)
                  .flatMap((pagina) => pagina?.items ?? [])
                  .find((tarefa) => tarefa.id === evento.active.id) ?? null,
              )
            }
            onDragCancel={() => definirArrastada(null)}
            onDragEnd={terminarArraste}
            accessibility={{
              screenReaderInstructions: {
                draggable:
                  "Pressione espaço para pegar a tarefa, use as setas para mover e espaço para soltar. Escape cancela.",
              },
              announcements: {
                onDragStart: () => "Tarefa selecionada para mover.",
                onDragOver: ({ over }) =>
                  over ? "Destino selecionado." : "Fora do quadro.",
                onDragEnd: () => "Movimento encerrado.",
                onDragCancel: () => "Movimento cancelado.",
              },
            }}
          >
            <div className="task-board" aria-busy={ocupada}>
              {estados
                .filter(
                  (status) => !filtros.status || status === filtros.status,
                )
                .map((status) => (
                  <ColunaTarefas
                    key={status}
                    status={status}
                    pagina={colunas[status]}
                    visivel={colunaMobile === status}
                    ocupada={ocupada}
                    arrasteAtivo={arrasteAtivo}
                    aoAbrir={abrir}
                    aoMover={mover}
                    aoCarregar={() => void carregarMais(status)}
                  />
                ))}
            </div>
            <DragOverlay>
              {arrastada && (
                <div className="task-card">
                  <ConteudoCardTarefa tarefa={arrastada} />
                </div>
              )}
            </DragOverlay>
          </DndContext>
        </>
      )}
      {detalhe !== undefined && (
        <DetalheTarefa
          key={detalhe?.id ?? "nova"}
          inicial={detalhe}
          opcoes={opcoesIniciais}
          podeAtribuir={permissoes.includes("tasks.assign")}
          podeVerProjetos={permissoes.includes("projects.view")}
          aoFechar={() => {
            definirDetalhe(undefined);
            void carregar(false);
          }}
          aoConcluir={(mensagem) => {
            definirDetalhe(undefined);
            definirMensagem(mensagem);
            void carregar();
          }}
        />
      )}
      <Toast message={mensagem} onClose={() => definirMensagem("")} />
    </div>
  );
}
