import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/auth/session";
import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { LoginForm } from "@/components/login-form";

export const metadata = { title: "Entrar" };
export default async function LoginPage() {
  if (await getCurrentUser()) redirect("/workspace");
  return <WorkspaceShell><div className="grid w-full items-center gap-12 lg:grid-cols-[1.1fr_1fr] lg:gap-20">
    <div><Badge>Workspace interno</Badge><p className="mt-9 text-xs font-semibold tracking-[0.18em] text-primary uppercase">Um espaço nosso</p><h1 className="mt-4 text-4xl leading-[1.08] font-semibold tracking-tight sm:text-6xl">Bom ter você<br /><span className="text-primary">por aqui.</span></h1><p className="mt-6 max-w-md text-base leading-7 text-muted-foreground">Entre para acessar seu espaço na Tech Sisters. Juntas, fazemos acontecer.</p></div>
    <Card aria-labelledby="login-title" className="p-6 sm:p-8"><h2 id="login-title" className="text-2xl font-semibold tracking-tight">Entrar no workspace</h2><p className="mt-3 text-sm leading-6 text-muted-foreground">Use o e-mail e a senha do seu acesso.</p><LoginForm /><p className="mt-6 text-xs leading-5 text-muted-foreground">Acesso exclusivo para integrantes autorizadas da Tech Sisters.</p></Card>
  </div></WorkspaceShell>;
}
