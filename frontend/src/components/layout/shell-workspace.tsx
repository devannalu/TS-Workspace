import type { ReactNode } from "react";
import { exigirSessao } from "@/lib/sessao";
import { ShellAplicacao } from "./shell-aplicacao";
import { ProtecaoSessao } from "../autenticacao/protecao-sessao";
export async function ShellWorkspace({ children }: { children: ReactNode }) {
  const usuario = await exigirSessao();
  return (
    <ProtecaoSessao>
      <ShellAplicacao usuario={usuario}>{children}</ShellAplicacao>
    </ProtecaoSessao>
  );
}
