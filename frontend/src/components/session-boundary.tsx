"use client";
import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { getCurrentJavaUser } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/http";

export function SessionBoundary({children}:{children:ReactNode}) {
  const router=useRouter();
  const [state,setState]=useState<"loading"|"authenticated"|"unauthenticated"|"forbidden"|"unavailable">("loading");
  useEffect(()=>{
    let active=true;
    const check=()=>getCurrentJavaUser().then(user=>{
      if(!active)return;
      setState(user?"authenticated":"unauthenticated");
      if(!user)router.replace("/login");
    }).catch(error=>{if(active)setState(error instanceof ApiError&&error.status===403?"forbidden":"unavailable");});
    const expired=()=>{setState("unauthenticated");router.replace("/login");};
    void check(); window.addEventListener("focus",check); window.addEventListener("java-session-expired",expired);
    return ()=>{active=false;window.removeEventListener("focus",check);window.removeEventListener("java-session-expired",expired);};
  },[router]);
  if(state==="loading")return <p role="status">Verificando sessão…</p>;
  if(state==="unauthenticated")return <p role="status">Sessão encerrada. Redirecionando para entrar…</p>;
  if(state==="forbidden")return <p role="alert">Seu acesso está indisponível. Procure uma administradora.</p>;
  if(state==="unavailable")return <p role="alert">Não foi possível verificar sua sessão. Recarregue a página.</p>;
  return children;
}
