import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/auth/session";
import { PublicShell } from "@/components/layout/public-shell";
import { LoginForm } from "@/components/login-form";
export const metadata = { title: "Entrar" };
export default async function LoginPage() {
  if (await getCurrentUser()) redirect("/workspace");
  return (
    <PublicShell>
      <p className="text-xs font-medium uppercase tracking-widest text-primary">
        Workspace interno
      </p>
      <h2 className="mt-3 text-3xl font-semibold tracking-tight">
        Bem-vinda de volta.
      </h2>
      <p className="mt-3 subtle">
        Entre para acessar seu espaço na Tech Sisters.
      </p>
      <LoginForm />
      <p className="mt-6 border-t border-border pt-4 text-xs leading-5 text-muted-foreground">
        Acesso por convite, exclusivo para integrantes da Tech Sisters.
      </p>
    </PublicShell>
  );
}
