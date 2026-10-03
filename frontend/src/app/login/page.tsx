import { redirect } from "next/navigation";
import { buscarUsuarioDaSessao } from "@/lib/sessao";
import { ShellPublico } from "@/components/layout/shell-publico";
import { FormularioLogin } from "@/components/autenticacao/formulario-login";
export const metadata = { title: "Entrar" };
export default async function LoginPage() {
  if (await buscarUsuarioDaSessao()) redirect("/workspace");
  return (
    <ShellPublico>
      <p className="text-xs font-medium uppercase tracking-widest text-primary">
        Workspace interno
      </p>
      <h2 className="mt-3 text-3xl font-semibold tracking-tight">
        Bem-vinda de volta.
      </h2>
      <p className="mt-3 subtle">
        Entre para acessar seu espaço na Tech Sisters.
      </p>
      <FormularioLogin />
      <p className="mt-6 border-t border-border pt-4 text-xs leading-5 text-muted-foreground">
        Acesso por convite, exclusivo para integrantes da Tech Sisters.
      </p>
    </ShellPublico>
  );
}
