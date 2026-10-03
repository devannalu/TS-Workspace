"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Mail, Link2Off } from "lucide-react";
import { validateJavaInvite, type JavaPublicInvite } from "@/lib/api/invites";
import { ApiError } from "@/lib/api/http";
import { InviteAcceptForm } from "./invite-accept-form";
import { Badge } from "./ui/badge";
import { Button } from "./ui/button";
export function PublicInvite({ token }: { token: string }) {
  const [state, setState] = useState<{
      invite?: JavaPublicInvite;
      error?: string;
      unavailable?: boolean;
    }>({}),
    [attempt, setAttempt] = useState(0);
  useEffect(() => {
    let active = true;
    validateJavaInvite(token)
      .then((invite) => {
        if (active) setState({ invite });
      })
      .catch((error) => {
        if (active) {
          const unavailable =
            !(error instanceof ApiError) ||
            error.status === 0 ||
            error.status >= 500;
          setState({
            unavailable,
            error: unavailable
              ? "Não foi possível verificar o convite. Confira sua conexão e tente novamente."
              : "Este convite não está disponível. Ele pode ter expirado, sido cancelado ou já utilizado.",
          });
        }
      });
    return () => {
      active = false;
    };
  }, [token, attempt]);
  if (state.error)
    return (
      <div>
        <span className="inline-flex rounded-xl bg-peach p-3">
          <Link2Off size={24} aria-hidden />
        </span>
        <h2 className="mt-5 text-2xl font-semibold">
          Não foi possível usar este convite.
        </h2>
        <p role="alert" className="mt-3 subtle">
          {state.error}
        </p>
        <p className="mt-3 subtle">
          Procure uma administradora para receber orientação.
        </p>
        {state.unavailable && (
          <Button
            className="mt-5"
            onClick={() => {
              setState({});
              setAttempt(attempt + 1);
            }}
          >
            Tentar novamente
          </Button>
        )}
        <Link
          href="/login"
          className="mt-6 inline-flex min-h-11 items-center text-sm font-semibold text-primary"
        >
          Ir para o login
        </Link>
      </div>
    );
  if (!state.invite)
    return (
      <div role="status" className="space-y-4">
        <p className="subtle">Verificando convite…</p>
        <div className="h-8 animate-pulse rounded-lg bg-muted" />
        <div className="h-48 animate-pulse rounded-xl bg-muted" />
      </div>
    );
  return (
    <>
      <span className="inline-flex rounded-xl bg-accent p-3">
        <Mail size={22} aria-hidden />
      </span>
      <h2 className="mt-4 text-2xl font-semibold">Você foi convidada.</h2>
      <p className="mt-2 subtle">Faça parte do Workspace da Tech Sisters.</p>
      <div className="mt-5 rounded-xl bg-muted p-4">
        <p className="break-all text-sm font-medium">{state.invite.email}</p>
        <div className="mt-2">
          <Badge tone="pink">{state.invite.role}</Badge>
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          {state.invite.teams.join(" · ")}
        </p>
      </div>
      <InviteAcceptForm token={token} />
    </>
  );
}
