"use client";
import { useEffect, useState } from "react";
import { validateJavaInvite, type JavaPublicInvite } from "@/lib/api/invites";
import { InviteAcceptForm } from "./invite-accept-form";

export function PublicInvite({token}:{token:string}) {
  const [state,setState]=useState<{invite?:JavaPublicInvite;error?:string}>({});
  useEffect(()=>{
    let active=true;
    validateJavaInvite(token).then(invite=>{if(active)setState({invite});})
      .catch(()=>{if(active)setState({error:"Convite inválido ou indisponível. Procure uma administradora."});});
    return ()=>{active=false;};
  },[token]);
  if(state.error) return <><h1 className="mt-5 text-2xl font-semibold">Não foi possível usar este convite</h1><p role="alert" className="mt-3 text-muted-foreground">{state.error}</p></>;
  if(!state.invite) return <p role="status" className="mt-5">Verificando convite…</p>;
  return <><h1 className="mt-5 text-3xl font-semibold tracking-tight">Boas-vindas ao workspace</h1><p className="mt-3 text-muted-foreground">Você foi convidada para entrar como <strong>{state.invite.role}</strong> nas equipes {state.invite.teams.join(", ")}.</p><p className="mt-3 text-sm text-muted-foreground">E-mail convidado: <strong>{state.invite.email}</strong></p><InviteAcceptForm token={token}/></>;
}
