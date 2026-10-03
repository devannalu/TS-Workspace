"use client";
import { useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  Plus,
  Network,
  Users,
  ChevronRight,
  Pencil,
  Archive,
} from "lucide-react";
import { DialogosEquipe, type EquipeResumo, type UsuarioEquipe } from "./dialogos-equipe";
import { Button } from "../ui/button";
import { Badge } from "../ui/badge";
import { Toast, EmptyState } from "../ui/feedback";
export function GestaoEquipes({
  equipes,
  usuarios,
  podeCriar,
  podeEditar,
  podeArquivar,
  podeGerenciarIntegrantes,
  abrirCriacaoInicial = false,
}: {
  equipes: EquipeResumo[];
  usuarios: UsuarioEquipe[];
  podeCriar: boolean;
  podeEditar: boolean;
  podeArquivar: boolean;
  podeGerenciarIntegrantes: boolean;
  abrirCriacaoInicial?: boolean;
}) {
  const router = useRouter();
  const [mensagemSucesso, definirMensagemSucesso] = useState("");
  const [modo, definirModo] = useState<
    "create" | "edit" | "members" | "archive" | null
  >(podeCriar && abrirCriacaoInicial ? "create" : null);
  const [equipeSelecionada, definirEquipeSelecionada] = useState<EquipeResumo | null>(null);
  const fecharFeedback = useCallback(() => definirMensagemSucesso(""), []);
  const abrirDialogoEquipe = (modoDialogo: typeof modo, equipe: EquipeResumo) => {
    definirEquipeSelecionada(equipe);
    definirModo(modoDialogo);
  };
  // A ordem mostra as equipes superiores antes das suas filhas.
  const equipesOrdenadas: { team: EquipeResumo; depth: number }[] = [];
  const ordenarSubequipes = (equipeMaeId: string | null, nivel: number) => {
    for (const team of equipes.filter((t) => t.parentId === equipeMaeId)) {
      equipesOrdenadas.push({ team, depth: nivel });
      ordenarSubequipes(team.id, nivel + 1);
    }
  };
  ordenarSubequipes(null, 0);
  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="page-title">Equipes</h1>
          <p className="mt-2 subtle">
            Organize as áreas que fazem a Tech Sisters acontecer.
          </p>
        </div>
        {podeCriar && (
          <Button
            onClick={(event) => {
              event.currentTarget.focus();
              definirModo("create");
            }}
          >
            <Plus size={17} aria-hidden />
            Nova equipe
          </Button>
        )}
      </div>
      <div className="rounded-xl bg-lilac px-4 py-3 text-sm">
        Fundadoras é a raiz estrutural. Cada área tem seu espaço para construir
        juntas.
      </div>
      {!equipesOrdenadas.length && (
        <EmptyState
          title="Nenhuma equipe disponível"
          description="As equipes ativas aparecerão aqui."
        />
      )}
      <div className="space-y-3">
        {equipesOrdenadas.map(({ team, depth }) => (
          <article
            key={team.id}
            className={`workspace-panel p-5 ${depth ? "sm:ml-6" : ""}`}
          >
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div className="flex min-w-0 gap-3">
                <span className="mt-1 rounded-xl bg-lilac p-2.5">
                  <Network size={20} aria-hidden />
                </span>
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="font-semibold">{team.name}</h2>
                    <Badge tone={team.parentId ? "success" : "pink"}>
                      {team.parentId ? "Ativa" : "Raiz estrutural"}
                    </Badge>
                  </div>
                  <p className="mt-1 subtle">
                    {team.description || "Sem descrição."}
                  </p>
                  <p className="mt-3 flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                    <Users size={14} aria-hidden />
                    {team.memberCount}{" "}
                    {team.memberCount === 1 ? "integrante" : "integrantes"}
                    {team.parentId && (
                      <>
                        <ChevronRight size={14} aria-hidden />
                        {equipes.find((t) => t.id === team.parentId)?.name}
                      </>
                    )}
                  </p>
                </div>
              </div>
              <div className="flex flex-wrap gap-1">
                {podeEditar && team.parentId && (
                  <Button
                    variant="ghost"
                    aria-label={`Editar ${team.name}`}
                    onClick={(event) => {
                      event.currentTarget.focus();
                      abrirDialogoEquipe("edit", team);
                    }}
                  >
                    <Pencil size={15} aria-hidden />
                    Editar
                  </Button>
                )}
                {podeGerenciarIntegrantes && (
                  <Button
                    variant="secondary"
                    aria-label={`Gerenciar integrantes de ${team.name}`}
                    onClick={(event) => {
                      event.currentTarget.focus();
                      abrirDialogoEquipe("members", team);
                    }}
                  >
                    Integrantes
                  </Button>
                )}
                {podeArquivar && team.parentId && (
                  <Button
                    variant="ghost"
                    aria-label={`Arquivar ${team.name}`}
                    onClick={(event) => {
                      event.currentTarget.focus();
                      abrirDialogoEquipe("archive", team);
                    }}
                  >
                    <Archive size={15} aria-hidden />
                    Arquivar
                  </Button>
                )}
              </div>
            </div>
          </article>
        ))}
      </div>
      <DialogosEquipe
        key={`${modo}-${equipeSelecionada?.id ?? "new"}`}
        modo={modo}
        equipeSelecionada={equipeSelecionada}
        equipes={equipes}
        usuarios={usuarios}
        onClose={() => {
          definirModo(null);
          definirEquipeSelecionada(null);
        }}
        onSaved={(message) => {
          definirMensagemSucesso(message);
          router.refresh();
        }}
      />
      <Toast message={mensagemSucesso} onClose={fecharFeedback} />
    </div>
  );
}
