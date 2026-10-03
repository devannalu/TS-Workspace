"use client";
import { useState, useRef, useEffect, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { House, Users, Network, Menu, ChevronDown } from "lucide-react";
import type { JavaUser } from "@/lib/api/auth";
import { navigationFor } from "@/lib/ui/permissions";
import { Avatar } from "../ui/avatar";
import { Dialog } from "../ui/dialog";
import { Brand } from "./public-shell";
import { LogoutButton } from "../logout-button";
const icons = { Início: House, Equipes: Network, Usuárias: Users };
export function AppShell({
  user,
  children,
}: {
  user: JavaUser;
  children: ReactNode;
}) {
  const path = usePathname(),
    [mobile, setMobile] = useState(false),
    menuRef = useRef<HTMLDetailsElement>(null);
  const nav = navigationFor(user.permissions),
    title =
      nav.find((item) => path.startsWith(item.href))?.label ?? "Workspace";
  useEffect(() => {
    const close = (event: KeyboardEvent) => {
      if (event.key === "Escape" && menuRef.current?.open) {
        menuRef.current.open = false;
        menuRef.current.querySelector("summary")?.focus();
      }
    };
    const outside = (event: PointerEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node))
        menuRef.current.open = false;
    };
    document.addEventListener("keydown", close);
    document.addEventListener("pointerdown", outside);
    return () => {
      document.removeEventListener("keydown", close);
      document.removeEventListener("pointerdown", outside);
    };
  }, []);
  const links = (
    <nav aria-label="Navegação principal" className="space-y-1">
      {nav.map((item) => {
        const Icon = icons[item.label as keyof typeof icons];
        return (
          <Link
            key={item.href}
            href={item.href}
            aria-current={path === item.href ? "page" : undefined}
            className="sidebar-link"
            onClick={() => setMobile(false)}
          >
            <Icon size={18} aria-hidden />
            {item.label}
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
          <Brand />
        </div>
        <div className="mt-10">{links}</div>
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
              aria-expanded={mobile}
              className="flex size-11 items-center justify-center rounded-xl hover:bg-muted lg:hidden"
              onClick={() => setMobile(true)}
            >
              <Menu size={20} aria-hidden />
            </button>
            <div>
              <p className="text-xs text-muted-foreground">Seu espaço /</p>
              <p className="text-sm font-semibold">{title}</p>
            </div>
          </div>
          <details ref={menuRef} className="relative">
            <summary
              aria-label="Menu da usuária"
              className="flex min-h-11 list-none items-center gap-2 rounded-xl p-1 hover:bg-muted"
            >
              <Avatar name={user.name} />
              <div className="hidden text-left sm:block">
                <p className="text-sm font-medium">{user.name}</p>
                <p className="text-xs text-muted-foreground">
                  {user.role.name}
                </p>
              </div>
              <ChevronDown size={15} aria-hidden />
            </summary>
            <div className="absolute top-full right-0 z-30 mt-2 w-64 rounded-xl border border-border bg-card p-4 shadow-lg">
              <p className="font-medium">{user.name}</p>
              <p className="mt-1 break-all text-xs text-muted-foreground">
                {user.email}
              </p>
              <p className="mt-2 text-xs">{user.role.name}</p>
              <div className="mt-4 border-t border-border pt-3">
                <LogoutButton />
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
        open={mobile}
        onClose={() => setMobile(false)}
        title="TS Workspace"
        description="Tech Sisters"
        placement="drawer"
      >
        {links}
      </Dialog>
    </div>
  );
}
