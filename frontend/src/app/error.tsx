"use client";
import Link from "next/link";
import { AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
export default function ErrorPage({ reset }: { reset: () => void }) {
  return (
    <main className="flex min-h-dvh items-center justify-center p-6">
      <div className="workspace-panel max-w-lg p-7">
        <AlertCircle size={28} aria-hidden className="text-primary" />
        <p className="mt-4 text-xs text-muted-foreground">TS WORKSPACE</p>
        <h1 className="mt-3 text-2xl font-semibold">
          Não foi possível carregar este espaço.
        </h1>
        <p className="mt-3 subtle">
          Verifique sua conexão e tente novamente. Se o problema continuar,
          procure uma administradora.
        </p>
        <div className="mt-6 flex flex-wrap gap-3">
          <Button onClick={reset}>Tentar novamente</Button>
          <Link
            className="inline-flex min-h-11 items-center px-4 text-sm font-medium"
            href="/login"
          >
            Voltar ao login
          </Link>
        </div>
      </div>
    </main>
  );
}
