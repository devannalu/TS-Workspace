"use client";
import { useRouter, useSearchParams } from "next/navigation";
import type { JavaInvite } from "@/lib/api/invites";
import type { JavaPage } from "@/lib/api/management";
import { friendlyDate, inviteLabels } from "@/lib/ui/format";
import { Badge } from "./ui/badge";
import { Pagination } from "./ui/pagination";
import { EmptyState } from "./ui/feedback";
import { CancelInviteButton } from "./cancel-invite-button";
export function InviteList({
  data,
  canCancel,
}: {
  data: JavaPage<JavaInvite>;
  canCancel: boolean;
}) {
  const router = useRouter(),
    query = useSearchParams();
  return (
    <div className="space-y-4">
      {!data.items.length ? (
        <EmptyState
          title="Nenhum convite por aqui"
          description="Os convites criados aparecerão aqui com seu status e validade."
        />
      ) : (
        <div className="space-y-3">
          {data.items.map((invite) => (
            <article
              key={invite.id}
              className="flex flex-wrap items-start justify-between gap-4 rounded-xl border border-border p-4"
            >
              <div className="min-w-0">
                <p className="break-all text-sm font-medium">{invite.email}</p>
                <p className="mt-1 subtle">
                  {invite.role.name} ·{" "}
                  {invite.teams.map((t) => t.name).join(", ")}
                </p>
                <p className="mt-2 text-xs leading-5 text-muted-foreground">
                  Enviado em {friendlyDate(invite.createdAt)} · Expira em{" "}
                  {friendlyDate(invite.expiresAt)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Badge
                  tone={
                    invite.status === "PENDING"
                      ? "warning"
                      : invite.status === "USED"
                        ? "success"
                        : "neutral"
                  }
                >
                  {inviteLabels[invite.status]}
                </Badge>
                {invite.status === "PENDING" && canCancel && (
                  <CancelInviteButton inviteId={invite.id} />
                )}
              </div>
            </article>
          ))}
        </div>
      )}
      <Pagination
        page={data.page}
        size={data.size}
        total={data.total}
        label="convites"
        onPage={(page) => {
          const next = new URLSearchParams(query.toString());
          next.set("invitePage", String(page));
          router.push("/usuarias?" + next);
        }}
      />
    </div>
  );
}
