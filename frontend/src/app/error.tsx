"use client";
import { Button } from "@/components/ui/button";

export default function ErrorPage({ reset }: { reset: () => void }) {
  return <main className="mx-auto flex min-h-dvh max-w-lg flex-col items-start justify-center gap-5 px-6"><h1 className="text-2xl font-semibold">Não foi possível carregar este espaço.</h1><p className="text-muted-foreground">Tente novamente em instantes. Se o problema continuar, procure uma administradora.</p><Button onClick={reset}>Tentar novamente</Button></main>;
}
