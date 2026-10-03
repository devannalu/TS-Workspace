import Link from "next/link";
import { Compass } from "lucide-react";
export default function NotFound() {
  return (
    <main className="flex min-h-dvh items-center justify-center p-6">
      <div className="workspace-panel max-w-md p-8">
        <Compass size={32} aria-hidden className="text-primary" />
        <p className="mt-5 text-xs text-muted-foreground">TS WORKSPACE / 404</p>
        <h1 className="mt-3 text-2xl font-semibold">
          Este caminho não existe.
        </h1>
        <p className="mt-3 subtle">Vamos voltar ao seu espaço?</p>
        <Link
          href="/workspace"
          className="mt-6 inline-flex min-h-11 items-center rounded-xl bg-primary px-5 text-sm font-semibold text-primary-foreground"
        >
          Ir para o início
        </Link>
      </div>
    </main>
  );
}
