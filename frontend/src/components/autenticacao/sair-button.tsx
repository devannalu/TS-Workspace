"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { sairDoWorkspace } from "@/lib/api/autenticacao";
import { Button } from "../ui/button";

export function SairButton() {
  const router = useRouter();
  const [saindo, definirSaindo] = useState(false);
  const [houveErro, definirHouveErro] = useState(false);
  async function sair() {
    definirSaindo(true); definirHouveErro(false);
    try {
      await sairDoWorkspace();
      router.replace("/login");
      router.refresh();
    } catch { definirHouveErro(true); definirSaindo(false); }
  }
  return <div><Button onClick={sair} disabled={saindo}>{saindo ? "Saindo…" : "Sair"}</Button>{houveErro && <p role="alert" className="mt-2 text-sm text-danger">Não foi possível sair. Tente novamente.</p>}</div>;
}
