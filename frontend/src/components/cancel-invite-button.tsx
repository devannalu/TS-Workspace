"use client";
import { useState, useTransition, useCallback } from "react";
import { useRouter } from "next/navigation";
import { cancelInviteAction } from "@/lib/api/ui-actions";
import { Button } from "./ui/button";
import { Dialog } from "./ui/dialog";
import { Toast } from "./ui/feedback";
export function CancelInviteButton({ inviteId }: { inviteId: string }) {
  const router = useRouter(),
    [open, setOpen] = useState(false),
    [pending, start] = useTransition(),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const dismiss = useCallback(() => setMessage(""), []);
  return (
    <>
      <Button
        variant="ghost"
        onClick={() => {
          setError("");
          setOpen(true);
        }}
      >
        Cancelar
      </Button>
      <Dialog
        open={open}
        onClose={() => {
          if (!pending) setOpen(false);
        }}
        busy={pending}
        title="Cancelar convite"
        description="O link deixará de permitir a criação de um acesso. Esta ação não pode ser desfeita."
      >
        {error && (
          <p role="alert" className="feedback-error">
            {error}
          </p>
        )}
        <div className="mt-4 flex justify-end gap-2">
          <Button
            variant="secondary"
            disabled={pending}
            onClick={() => setOpen(false)}
          >
            Voltar
          </Button>
          <Button
            variant="danger"
            disabled={pending}
            onClick={() =>
              start(async () => {
                const result = await cancelInviteAction(inviteId);
                if (!result.ok) setError(result.error);
                else {
                  setOpen(false);
                  setMessage("Convite cancelado.");
                  router.refresh();
                }
              })
            }
          >
            {pending ? "Cancelando…" : "Confirmar cancelamento"}
          </Button>
        </div>
      </Dialog>
      <Toast message={message} onClose={dismiss} />
    </>
  );
}
