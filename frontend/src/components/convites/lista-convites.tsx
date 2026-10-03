"use client";
import { useRouter, useSearchParams } from "next/navigation";
import type { Convite } from "@/lib/api/convites";
import type { PaginaApi } from "@/lib/api/contratos";
import { formatarData, rotulosSituacaoConvite } from "@/lib/ui/formatacao";
import { Badge } from "../ui/badge";
import { Pagination } from "../ui/pagination";
import { EmptyState } from "../ui/feedback";
import { CancelarConviteButton } from "./cancelar-convite-button";
export function ListaConvites({
  paginaConvites,
  podeCancelar,
}: {
  paginaConvites: PaginaApi<Convite>;
  podeCancelar: boolean;
}) {
  const router = useRouter(),
    parametrosBusca = useSearchParams();
  return (
    <div className="space-y-4">
      {!paginaConvites.items.length ? (
        <EmptyState
          title="Nenhum convite por aqui"
          description="Os convites criados aparecerão aqui com seu status e validade."
        />
      ) : (
        <div className="space-y-3">
          {paginaConvites.items.map((convite) => (
            <article
              key={convite.id}
              className="flex flex-wrap items-start justify-between gap-4 rounded-xl border border-border p-4"
            >
              <div className="min-w-0">
                <p className="break-all text-sm font-medium">{convite.email}</p>
                <p className="mt-1 subtle">
                  {convite.role.name} ·{" "}
                  {convite.teams.map((t) => t.name).join(", ")}
                </p>
                <p className="mt-2 text-xs leading-5 text-muted-foreground">
                  Enviado em {formatarData(convite.createdAt)} · Expira em{" "}
                  {formatarData(convite.expiresAt)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Badge
                  tone={
                    convite.status === "PENDING"
                      ? "warning"
                      : convite.status === "USED"
                        ? "success"
                        : "neutral"
                  }
                >
                  {rotulosSituacaoConvite[convite.status]}
                </Badge>
                {convite.status === "PENDING" && podeCancelar && (
                  <CancelarConviteButton conviteId={convite.id} />
                )}
              </div>
            </article>
          ))}
        </div>
      )}
      <Pagination
        page={paginaConvites.page}
        size={paginaConvites.size}
        total={paginaConvites.total}
        label="convites"
        onPage={(pagina) => {
          const proximosParametros = new URLSearchParams(parametrosBusca.toString());
          proximosParametros.set("invitePage", String(pagina));
          router.push("/usuarias?" + proximosParametros);
        }}
      />
    </div>
  );
}
