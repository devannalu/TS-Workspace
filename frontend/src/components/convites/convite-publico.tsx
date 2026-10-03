"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Mail, Link2Off } from "lucide-react";
import { consultarConvitePublico, type ConvitePublicoResponse } from "@/lib/api/convites";
import { ErroApi } from "@/lib/api/http";
import { FormularioAceiteConvite } from "./formulario-aceite-convite";
import { Badge } from "../ui/badge";
import { Button } from "../ui/button";
export function ConvitePublico({ token }: { token: string }) {
  const [estadoValidacao, definirEstadoValidacao] = useState<{
      convite?: ConvitePublicoResponse;
      mensagemErro?: string;
      servicoIndisponivel?: boolean;
    }>({}),
    [tentativaValidacao, definirTentativaValidacao] = useState(0);
  useEffect(() => {
    let componenteAtivo = true;
    consultarConvitePublico(token)
      .then((convite) => {
        if (componenteAtivo) definirEstadoValidacao({ convite: convite });
      })
      .catch((mensagemErro) => {
        if (componenteAtivo) {
          const servicoIndisponivel =
            !(mensagemErro instanceof ErroApi) ||
            mensagemErro.status === 0 ||
            mensagemErro.status >= 500;
          definirEstadoValidacao({
            servicoIndisponivel: servicoIndisponivel,
            mensagemErro: servicoIndisponivel
              ? "Não foi possível verificar o convite. Confira sua conexão e tente novamente."
              : "Este convite não está disponível. Ele pode ter expirado, sido cancelado ou já utilizado.",
          });
        }
      });
    return () => {
      componenteAtivo = false;
    };
  }, [token, tentativaValidacao]);
  if (estadoValidacao.mensagemErro)
    return (
      <div>
        <span className="inline-flex rounded-xl bg-peach p-3">
          <Link2Off size={24} aria-hidden />
        </span>
        <h2 className="mt-5 text-2xl font-semibold">
          Não foi possível usar este convite.
        </h2>
        <p role="alert" className="mt-3 subtle">
          {estadoValidacao.mensagemErro}
        </p>
        <p className="mt-3 subtle">
          Procure uma administradora para receber orientação.
        </p>
        {estadoValidacao.servicoIndisponivel && (
          <Button
            className="mt-5"
            onClick={() => {
              definirEstadoValidacao({});
              definirTentativaValidacao(tentativaValidacao + 1);
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
  if (!estadoValidacao.convite)
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
        <p className="break-all text-sm font-medium">{estadoValidacao.convite.email}</p>
        <div className="mt-2">
          <Badge tone="pink">{estadoValidacao.convite.role}</Badge>
        </div>
        <p className="mt-2 text-xs text-muted-foreground">
          {estadoValidacao.convite.teams.join(" · ")}
        </p>
      </div>
      <FormularioAceiteConvite token={token} />
    </>
  );
}
