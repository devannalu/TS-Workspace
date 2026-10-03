import { ShellPublico } from "@/components/layout/shell-publico";
export default function Loading() {
  return (
    <ShellPublico>
      <p role="status" className="subtle">
        Verificando convite…
      </p>
      <div className="mt-5 h-48 animate-pulse rounded-xl bg-muted" />
    </ShellPublico>
  );
}
