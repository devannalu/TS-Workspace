"use client";
import { useState, useTransition, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Mail, Copy, Check } from "lucide-react";
import { criarConviteComFeedback } from "@/lib/api/acoes";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Dialog } from "../ui/dialog";
import { Toast } from "../ui/feedback";
type OpcaoConvite = { id: string; name: string };
export function FormularioConvite({
  perfisAcesso,
  equipes,
  inicialmenteAberto = false,
}: {
  perfisAcesso: OpcaoConvite[];
  equipes: OpcaoConvite[];
  inicialmenteAberto?: boolean;
}) {
  const router = useRouter(),
    [dialogoAberto, definirDialogoAberto] = useState(inicialmenteAberto),
    [salvando, iniciarTransicao] = useTransition(),
    [mensagemErro, definirMensagemErro] = useState(""),
    [urlConvite, definirUrlConvite] = useState(""),
    [linkCopiado, definirLinkCopiado] = useState(false),
    [mensagemSucesso, definirMensagemSucesso] = useState("");
  const fecharFeedback = useCallback(() => definirMensagemSucesso(""), []);
  const fecharDialogo = () => {
    if (!salvando || urlConvite) {
      definirDialogoAberto(false);
      definirUrlConvite("");
      definirMensagemErro("");
      definirLinkCopiado(false);
    }
  };
  return (
    <>
      <Button
        onClick={() => {
          definirDialogoAberto(true);
          definirMensagemErro("");
        }}
      >
        <Mail size={17} aria-hidden />
        Convidar usuária
      </Button>
      <Dialog
        open={dialogoAberto}
        onClose={fecharDialogo}
        busy={salvando && !urlConvite}
        title={urlConvite ? "Convite criado" : "Convidar usuária"}
        description={
          urlConvite
            ? "Copie este link agora. Por segurança, ele não será exibido novamente."
            : "Escolha o perfil de acesso e as equipes da nova integrante."
        }
      >
        {urlConvite ? (
          <div className="space-y-4">
            <label className="field">
              Link de convite
              <Input
                readOnly
                value={urlConvite}
                onFocus={(event) => event.currentTarget.select()}
              />
            </label>
            <p className="subtle">
              O link é pessoal e permite criar um acesso. Compartilhe apenas com
              a pessoa convidada.
            </p>
            <Button
              onClick={async () => {
                try {
                  await navigator.clipboard.writeText(urlConvite);
                  definirLinkCopiado(true);
                  definirMensagemSucesso("Link copiado.");
                } catch {
                  definirMensagemErro(
                    "Não foi possível copiar. Selecione o link e copie manualmente.",
                  );
                }
              }}
            >
              {linkCopiado ? (
                <Check size={17} aria-hidden />
              ) : (
                <Copy size={17} aria-hidden />
              )}
              {linkCopiado ? "Copiado" : "Copiar link"}
            </Button>
            {mensagemErro && (
              <p role="alert" className="feedback-error">
                {mensagemErro}
              </p>
            )}
            <div className="border-t border-border pt-4">
              <Button variant="secondary" onClick={fecharDialogo}>
                Concluir
              </Button>
            </div>
          </div>
        ) : (
          <form
            className="space-y-4"
            aria-busy={salvando}
            onSubmit={(event) => {
              event.preventDefault();
              definirMensagemErro("");
              const dadosFormulario = new FormData(event.currentTarget),
                teamIds = dadosFormulario.getAll("teamIds").map(String);
              if (!teamIds.length) {
                definirMensagemErro("Selecione ao menos uma equipe.");
                return;
              }
              iniciarTransicao(async () => {
                const resultadoOperacao = await criarConviteComFeedback({
                  email: String(dadosFormulario.get("email")).trim(),
                  roleId: String(dadosFormulario.get("roleId")),
                  teamIds,
                });
                if (!resultadoOperacao.ok) definirMensagemErro(resultadoOperacao.error);
                else {
                  definirUrlConvite(resultadoOperacao.url);
                  definirMensagemSucesso("Convite criado.");
                  router.refresh();
                }
              });
            }}
          >
            <label className="field">
              E-mail
              <Input
                name="email"
                type="email"
                autoComplete="off"
                required
                maxLength={254}
                disabled={salvando}
              />
            </label>
            <label className="field">
              Perfil de acesso
              <select name="roleId" required disabled={salvando}>
                <option value="">Selecione um perfil</option>
                {perfisAcesso.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </label>
            <fieldset>
              <legend className="mb-2 text-sm font-medium">Equipes</legend>
              <div className="space-y-2">
                {equipes.map((t) => (
                  <label key={t.id} className="flex items-center gap-2 text-sm">
                    <input
                      type="checkbox"
                      name="teamIds"
                      value={t.id}
                      disabled={salvando}
                    />
                    {t.name}
                  </label>
                ))}
              </div>
            </fieldset>
            {mensagemErro && (
              <p role="alert" className="feedback-error">
                {mensagemErro}
              </p>
            )}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={fecharDialogo} disabled={salvando}>
                Cancelar
              </Button>
              <Button type="submit" disabled={salvando}>
                {salvando ? "Criando…" : "Criar convite"}
              </Button>
            </div>
          </form>
        )}
      </Dialog>
      <Toast message={mensagemSucesso} onClose={fecharFeedback} />
    </>
  );
}
