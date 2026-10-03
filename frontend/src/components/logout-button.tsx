"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { logoutJava } from "@/lib/api/auth";
import { Button } from "./ui/button";

export function LogoutButton() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(false);
  async function logout() {
    setPending(true); setError(false);
    try {
      await logoutJava();
      router.replace("/login");
      router.refresh();
    } catch { setError(true); setPending(false); }
  }
  return <div><Button onClick={logout} disabled={pending}>{pending ? "Saindo…" : "Sair"}</Button>{error && <p role="alert" className="mt-2 text-sm text-danger">Não foi possível sair. Tente novamente.</p>}</div>;
}
