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
import { TeamDialogs, type Team } from "./team-dialogs";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Toast, EmptyState } from "./ui/feedback";
type User = { id: string; name: string; email: string };
export function TeamManagement({
  teams,
  users,
  canCreate,
  canEdit,
  canArchive,
  canMembers,
  initialCreate = false,
}: {
  teams: Team[];
  users: User[];
  canCreate: boolean;
  canEdit: boolean;
  canArchive: boolean;
  canMembers: boolean;
  initialCreate?: boolean;
}) {
  const router = useRouter();
  const [message, setMessage] = useState("");
  const [mode, setMode] = useState<
    "create" | "edit" | "members" | "archive" | null
  >(canCreate && initialCreate ? "create" : null);
  const [selected, setSelected] = useState<Team | null>(null);
  const dismiss = useCallback(() => setMessage(""), []);
  const openTeamDialog = (dialog: typeof mode, team: Team) => {
    setSelected(team);
    setMode(dialog);
  };
  // A ordem mostra as equipes superiores antes das suas filhas.
  const order: { team: Team; depth: number }[] = [];
  const visit = (parent: string | null, depth: number) => {
    for (const team of teams.filter((t) => t.parentId === parent)) {
      order.push({ team, depth });
      visit(team.id, depth + 1);
    }
  };
  visit(null, 0);
  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="page-title">Equipes</h1>
          <p className="mt-2 subtle">
            Organize as áreas que fazem a Tech Sisters acontecer.
          </p>
        </div>
        {canCreate && (
          <Button
            onClick={(event) => {
              event.currentTarget.focus();
              setMode("create");
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
      {!order.length && (
        <EmptyState
          title="Nenhuma equipe disponível"
          description="As equipes ativas aparecerão aqui."
        />
      )}
      <div className="space-y-3">
        {order.map(({ team, depth }) => (
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
                        {teams.find((t) => t.id === team.parentId)?.name}
                      </>
                    )}
                  </p>
                </div>
              </div>
              <div className="flex flex-wrap gap-1">
                {canEdit && team.parentId && (
                  <Button
                    variant="ghost"
                    aria-label={`Editar ${team.name}`}
                    onClick={(event) => {
                      event.currentTarget.focus();
                      openTeamDialog("edit", team);
                    }}
                  >
                    <Pencil size={15} aria-hidden />
                    Editar
                  </Button>
                )}
                {canMembers && (
                  <Button
                    variant="secondary"
                    aria-label={`Gerenciar integrantes de ${team.name}`}
                    onClick={(event) => {
                      event.currentTarget.focus();
                      openTeamDialog("members", team);
                    }}
                  >
                    Integrantes
                  </Button>
                )}
                {canArchive && team.parentId && (
                  <Button
                    variant="ghost"
                    aria-label={`Arquivar ${team.name}`}
                    onClick={(event) => {
                      event.currentTarget.focus();
                      openTeamDialog("archive", team);
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
      <TeamDialogs
        key={`${mode}-${selected?.id ?? "new"}`}
        mode={mode}
        selected={selected}
        teams={teams}
        users={users}
        onClose={() => {
          setMode(null);
          setSelected(null);
        }}
        onSaved={(message) => {
          setMessage(message);
          router.refresh();
        }}
      />
      <Toast message={message} onClose={dismiss} />
    </div>
  );
}
