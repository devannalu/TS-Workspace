"use client";
import { useState, useTransition, useCallback } from "react";
import { useRouter } from "next/navigation";
import { cancelarConviteComFeedback } from "@/lib/api/acoes";
import { Button } from "../ui/button";
import { Dialog } from "../ui/dialog";
import { Toast } from "../ui/feedback";
export function CancelarConviteButton({ conviteId }: { conviteId: string }) {
  const router = useRouter(),
    [dialogoAberto, definirDialogoAberto] = useState(false),
    [salvando, iniciarTransicao] = useTransition(),
    [mensagemErro, definirMensagemErro] = useState(""),
    [mensagemSucesso, definirMensagemSucesso] = useState("");
  const fecharFeedback = useCallback(() => definirMensagemSucesso(""), []);
  return (
    <>
      <Button
        variant="ghost"
        onClick={() => {
          definirMensagemErro("");
          definirDialogoAberto(true);
        }}
      >
        Cancelar
      </Button>
      <Dialog
        open={dialogoAberto}
        onClose={() => {
          if (!salvando) definirDialogoAberto(false);
        }}
        busy={salvando}
        title="Cancelar convite"
        description="O link deixará de permitir a criação de um acesso. Esta ação não pode ser desfeita."
      >
        {mensagemErro && (
          <p role="alert" className="feedback-error">
            {mensagemErro}
          </p>
        )}
        <div className="mt-4 flex justify-end gap-2">
          <Button
            variant="secondary"
            disabled={salvando}
            onClick={() => definirDialogoAberto(false)}
          >
            Voltar
          </Button>
          <Button
            variant="danger"
            disabled={salvando}
            onClick={() =>
              iniciarTransicao(async () => {
                const resultadoOperacao = await cancelarConviteComFeedback(conviteId);
                if (!resultadoOperacao.ok) definirMensagemErro(resultadoOperacao.error);
                else {
                  definirDialogoAberto(false);
                  definirMensagemSucesso("Convite cancelado.");
                  router.refresh();
                }
              })
            }
          >
            {salvando ? "Cancelando…" : "Confirmar cancelamento"}
          </Button>
        </div>
      </Dialog>
      <Toast message={mensagemSucesso} onClose={fecharFeedback} />
    </>
  );
}
