import type { ReactNode } from "react";
import { AdminNav } from "./admin-nav";

export function WorkspaceShell({ children, canUsers = false, canTeams = false }: { children: ReactNode; canUsers?: boolean; canTeams?: boolean }) {
  return (
    <div className="foundation-surface flex min-h-dvh flex-col">
      <a href="#conteudo" className="sr-only fixed top-4 left-4 z-10 rounded-lg bg-primary px-4 py-3 text-primary-foreground focus:not-sr-only">Pular para o conteúdo</a>
      <header className="mx-auto flex w-full max-w-6xl items-center gap-3 px-6 py-7 sm:px-10">
        <span aria-hidden="true" className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary text-sm font-bold tracking-tight text-primary-foreground">ts.</span>
        <div>
          <p className="text-sm font-semibold tracking-tight">TS Workspace</p>
          <p className="mt-0.5 text-xs text-muted-foreground">Tech Sisters</p>
        </div>
        <div className="ml-auto"><AdminNav canUsers={canUsers} canTeams={canTeams} /></div>
      </header>
      <main id="conteudo" tabIndex={-1} className="mx-auto flex w-full max-w-6xl flex-1 items-center px-6 py-10 sm:px-10 sm:py-16">{children}</main>
      <footer className="mx-auto flex w-full max-w-6xl flex-col gap-2 px-6 py-7 text-xs text-muted-foreground sm:flex-row sm:justify-between sm:px-10">
        <p>Feito para conectar quem faz acontecer.</p>
        <p>Tech Sisters · Workspace interno</p>
      </footer>
    </div>
  );
}
