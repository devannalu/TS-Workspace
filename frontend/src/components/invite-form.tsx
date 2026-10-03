"use client";
import { useState, useTransition, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Mail, Copy, Check } from "lucide-react";
import { createInviteAction } from "@/lib/api/ui-actions";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Dialog } from "./ui/dialog";
import { Toast } from "./ui/feedback";
type Option = { id: string; name: string };
export function InviteForm({
  roles,
  teams,
  initialOpen = false,
}: {
  roles: Option[];
  teams: Option[];
  initialOpen?: boolean;
}) {
  const router = useRouter(),
    [open, setOpen] = useState(initialOpen),
    [pending, start] = useTransition(),
    [error, setError] = useState(""),
    [url, setUrl] = useState(""),
    [copied, setCopied] = useState(false),
    [message, setMessage] = useState("");
  const dismiss = useCallback(() => setMessage(""), []);
  const close = () => {
    if (!pending || url) {
      setOpen(false);
      setUrl("");
      setError("");
      setCopied(false);
    }
  };
  return (
    <>
      <Button
        onClick={() => {
          setOpen(true);
          setError("");
        }}
      >
        <Mail size={17} aria-hidden />
        Convidar usuária
      </Button>
      <Dialog
        open={open}
        onClose={close}
        busy={pending && !url}
        title={url ? "Convite criado" : "Convidar usuária"}
        description={
          url
            ? "Copie este link agora. Por segurança, ele não será exibido novamente."
            : "Escolha o perfil de acesso e as equipes da nova integrante."
        }
      >
        {url ? (
          <div className="space-y-4">
            <label className="field">
              Link de convite
              <Input
                readOnly
                value={url}
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
                  await navigator.clipboard.writeText(url);
                  setCopied(true);
                  setMessage("Link copiado.");
                } catch {
                  setError(
                    "Não foi possível copiar. Selecione o link e copie manualmente.",
                  );
                }
              }}
            >
              {copied ? (
                <Check size={17} aria-hidden />
              ) : (
                <Copy size={17} aria-hidden />
              )}
              {copied ? "Copiado" : "Copiar link"}
            </Button>
            {error && (
              <p role="alert" className="feedback-error">
                {error}
              </p>
            )}
            <div className="border-t border-border pt-4">
              <Button variant="secondary" onClick={close}>
                Concluir
              </Button>
            </div>
          </div>
        ) : (
          <form
            className="space-y-4"
            aria-busy={pending}
            onSubmit={(event) => {
              event.preventDefault();
              setError("");
              const form = new FormData(event.currentTarget),
                teamIds = form.getAll("teamIds").map(String);
              if (!teamIds.length) {
                setError("Selecione ao menos uma equipe.");
                return;
              }
              start(async () => {
                const result = await createInviteAction({
                  email: String(form.get("email")).trim(),
                  roleId: String(form.get("roleId")),
                  teamIds,
                });
                if (!result.ok) setError(result.error);
                else {
                  setUrl(result.url);
                  setMessage("Convite criado.");
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
                disabled={pending}
              />
            </label>
            <label className="field">
              Perfil de acesso
              <select name="roleId" required disabled={pending}>
                <option value="">Selecione um perfil</option>
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </label>
            <fieldset>
              <legend className="mb-2 text-sm font-medium">Equipes</legend>
              <div className="space-y-2">
                {teams.map((t) => (
                  <label key={t.id} className="flex items-center gap-2 text-sm">
                    <input
                      type="checkbox"
                      name="teamIds"
                      value={t.id}
                      disabled={pending}
                    />
                    {t.name}
                  </label>
                ))}
              </div>
            </fieldset>
            {error && (
              <p role="alert" className="feedback-error">
                {error}
              </p>
            )}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={close} disabled={pending}>
                Cancelar
              </Button>
              <Button type="submit" disabled={pending}>
                {pending ? "Criando…" : "Criar convite"}
              </Button>
            </div>
          </form>
        )}
      </Dialog>
      <Toast message={message} onClose={dismiss} />
    </>
  );
}
