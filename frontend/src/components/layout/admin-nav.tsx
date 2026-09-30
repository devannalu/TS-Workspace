import Link from "next/link";

export function AdminNav({ canUsers, canTeams }: { canUsers: boolean; canTeams: boolean }) {
  return <nav aria-label="Navegação principal" className="flex flex-wrap gap-2 text-sm">
    <Link className="rounded-lg px-3 py-2 hover:bg-muted" href="/workspace">Início</Link>
    {canTeams && <Link className="rounded-lg px-3 py-2 hover:bg-muted" href="/equipes">Equipes</Link>}
    {canUsers && <Link className="rounded-lg px-3 py-2 hover:bg-muted" href="/usuarias">Usuárias</Link>}
  </nav>;
}
