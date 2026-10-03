"use client";
import { useState, useRef, useEffect, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { House, Users, Network, Menu, ChevronDown } from "lucide-react";
import type { UsuarioAtual } from "@/lib/api/autenticacao";
import { navegacaoPermitida } from "@/lib/ui/permissoes";
import { Avatar } from "../ui/avatar";
import { Dialog } from "../ui/dialog";
import { MarcaWorkspace } from "./shell-publico";
import { SairButton } from "../autenticacao/sair-button";
const icones = { Início: House, Equipes: Network, Usuárias: Users };
export function ShellAplicacao({
  usuario,
  children,
}: {
  usuario: UsuarioAtual;
  children: ReactNode;
}) {
  const caminhoAtual = usePathname(),
    [menuMobileAberto, definirMenuMobileAberto] = useState(false),
    referenciaMenu = useRef<HTMLDetailsElement>(null);
  const navegacao = navegacaoPermitida(usuario.permissions),
    tituloPagina =
      navegacao.find((linkNavegacao) => caminhoAtual.startsWith(linkNavegacao.href))?.label ?? "Workspace";
  useEffect(() => {
    const fecharMenuComEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape" && referenciaMenu.current?.open) {
        referenciaMenu.current.open = false;
        referenciaMenu.current.querySelector("summary")?.focus();
      }
    };
    const fecharMenuAoClicarFora = (event: PointerEvent) => {
      if (referenciaMenu.current && !referenciaMenu.current.contains(event.target as Node))
        referenciaMenu.current.open = false;
    };
    document.addEventListener("keydown", fecharMenuComEscape);
    document.addEventListener("pointerdown", fecharMenuAoClicarFora);
    return () => {
      document.removeEventListener("keydown", fecharMenuComEscape);
      document.removeEventListener("pointerdown", fecharMenuAoClicarFora);
    };
  }, []);
  const linksNavegacao = (
    <nav aria-label="Navegação principal" className="space-y-1">
      {navegacao.map((linkNavegacao) => {
        const IconeNavegacao = icones[linkNavegacao.label as keyof typeof icones];
        return (
          <Link
            key={linkNavegacao.href}
            href={linkNavegacao.href}
            aria-current={caminhoAtual === linkNavegacao.href ? "page" : undefined}
            className="sidebar-link"
            onClick={() => definirMenuMobileAberto(false)}
          >
            <IconeNavegacao size={18} aria-hidden />
            {linkNavegacao.label}
          </Link>
        );
      })}
    </nav>
  );
  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[232px_minmax(0,1fr)]">
      <a
        href="#conteudo"
        className="sr-only fixed top-3 left-3 z-50 rounded-lg bg-primary px-4 py-3 text-primary-foreground focus:not-sr-only"
      >
        Pular para o conteúdo
      </a>
      <aside className="sticky top-0 hidden h-dvh flex-col border-r border-border bg-card px-4 py-6 lg:flex">
        <div className="px-2">
          <MarcaWorkspace />
        </div>
        <div className="mt-10">{linksNavegacao}</div>
        <p className="mt-auto px-3 text-xs leading-5 text-muted-foreground">
          Um espaço nosso.
          <br />
          Tech Sisters.
        </p>
      </aside>
      <div className="min-w-0">
        <header className="flex min-h-20 items-center justify-between gap-3 border-b border-border bg-card/80 px-4 sm:px-8">
          <div className="flex items-center gap-3">
            <button
              type="button"
              aria-label="Abrir menu"
              aria-expanded={menuMobileAberto}
              className="flex size-11 items-center justify-center rounded-xl hover:bg-muted lg:hidden"
              onClick={() => definirMenuMobileAberto(true)}
            >
              <Menu size={20} aria-hidden />
            </button>
            <div>
              <p className="text-xs text-muted-foreground">Seu espaço /</p>
              <p className="text-sm font-semibold">{tituloPagina}</p>
            </div>
          </div>
          <details ref={referenciaMenu} className="relative">
            <summary
              aria-label="Menu da usuária"
              className="flex min-h-11 list-none items-center gap-2 rounded-xl p-1 hover:bg-muted"
            >
              <Avatar name={usuario.name} />
              <div className="hidden text-left sm:block">
                <p className="text-sm font-medium">{usuario.name}</p>
                <p className="text-xs text-muted-foreground">
                  {usuario.role.name}
                </p>
              </div>
              <ChevronDown size={15} aria-hidden />
            </summary>
            <div className="absolute top-full right-0 z-30 mt-2 w-64 rounded-xl border border-border bg-card p-4 shadow-lg">
              <p className="font-medium">{usuario.name}</p>
              <p className="mt-1 break-all text-xs text-muted-foreground">
                {usuario.email}
              </p>
              <p className="mt-2 text-xs">{usuario.role.name}</p>
              <div className="mt-4 border-t border-border pt-3">
                <SairButton />
              </div>
            </div>
          </details>
        </header>
        <main
          id="conteudo"
          tabIndex={-1}
          className="mx-auto w-full max-w-[1440px] p-4 sm:p-8"
        >
          {children}
        </main>
      </div>
      <Dialog
        open={menuMobileAberto}
        onClose={() => definirMenuMobileAberto(false)}
        title="TS Workspace"
        description="Tech Sisters"
        placement="drawer"
      >
        {linksNavegacao}
      </Dialog>
    </div>
  );
}
