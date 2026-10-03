import { PublicShell } from "@/components/layout/public-shell";
export default function Loading() {
  return (
    <PublicShell>
      <p role="status" className="subtle">
        Verificando convite…
      </p>
      <div className="mt-5 h-48 animate-pulse rounded-xl bg-muted" />
    </PublicShell>
  );
}
