import type { ReactNode } from "react";
import { Sparkles, ArrowUpRight } from "lucide-react";
export function MarcaWorkspace() {
  return (
    <div className="flex items-center gap-3">
      <span
        aria-hidden
        className="flex size-10 items-center justify-center rounded-xl bg-accent text-lg font-bold text-accent-foreground"
      >
        ts.
      </span>
      <div>
        <p className="text-sm font-semibold">TS Workspace</p>
        <p className="text-xs text-muted-foreground">Tech Sisters</p>
      </div>
    </div>
  );
}
export function ShellPublico({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col">
      <header className="px-6 py-6 sm:px-10">
        <MarcaWorkspace />
      </header>
      <main
        id="conteudo"
        className="mx-auto grid w-full max-w-6xl flex-1 items-center gap-10 px-6 pb-10 lg:grid-cols-[1.05fr_1fr] lg:gap-20 lg:px-10"
      >
        <section className="hidden lg:block">
          <span className="inline-flex items-center gap-2 rounded-full bg-butter px-3 py-1.5 text-xs font-medium">
            <Sparkles size={14} aria-hidden />O nosso espaço digital
          </span>
          <h1 className="mt-6 text-5xl leading-[1.1] font-semibold tracking-tight">
            Ideias se encontram.
            <br />
            <span className="text-primary">A gente constrói.</span>
          </h1>
          <p className="mt-5 max-w-md subtle">
            O espaço interno da Tech Sisters para organizar, conectar e fazer
            acontecer juntas.
          </p>
          <div className="brand-grid mt-10 rounded-2xl border border-border p-6">
            <div className="workspace-panel bg-card p-5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-muted-foreground">
                  TECH SISTERS / WORKSPACE
                </span>
                <ArrowUpRight size={18} aria-hidden />
              </div>
              <p className="mt-8 text-lg font-medium">
                Feito por nós.
                <br />
                Para o que vem pela frente.
              </p>
              <div className="mt-6 flex gap-2">
                <span className="rounded-lg bg-accent px-3 py-1 text-xs">
                  Conectar
                </span>
                <span className="rounded-lg bg-lilac px-3 py-1 text-xs">
                  Construir
                </span>
                <span className="rounded-lg bg-butter px-3 py-1 text-xs">
                  Compartilhar
                </span>
              </div>
            </div>
          </div>
        </section>
        <section className="workspace-panel mx-auto w-full max-w-md p-6 sm:p-8">
          {children}
        </section>
      </main>
      <footer className="px-6 py-5 text-center text-xs text-muted-foreground">
        Tech Sisters · Feito para conectar quem faz acontecer.
      </footer>
    </div>
  );
}
