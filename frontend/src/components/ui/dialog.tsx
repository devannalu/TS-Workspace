"use client";
import { useEffect, useId, useRef, type ReactNode } from "react";
import { X } from "lucide-react";
export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  busy = false,
  placement = "center",
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  busy?: boolean;
  placement?: "center" | "drawer";
}) {
  const ref = useRef<HTMLDialogElement>(null),
    id = useId();
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (!open) {
      if (dialog.open) dialog.close();
      return;
    }
    const previousFocus = document.activeElement;
    if (!dialog.open) dialog.showModal();
    return () => {
      if (dialog.open) dialog.close();
      // O foco retorna depois que o diálogo sai da árvore de elementos.
      requestAnimationFrame(() => {
        if (previousFocus instanceof HTMLElement && previousFocus.isConnected)
          previousFocus.focus();
      });
    };
  }, [open]);
  return (
    <dialog
      ref={ref}
      className={
        placement === "drawer" ? "ui-dialog ui-drawer" : "ui-dialog m-auto"
      }
      aria-labelledby={id}
      aria-describedby={description ? id + "-description" : undefined}
      onKeyDown={(event) => {
        if (event.key !== "Tab") return;
        const fields = Array.from(
          event.currentTarget.querySelectorAll<HTMLElement>(
            'button:not(:disabled),a[href],input:not(:disabled),select:not(:disabled),textarea:not(:disabled),[tabindex="0"]',
          ),
        ).filter((field) => field.getClientRects().length > 0);
        const first = fields[0],
          last = fields.at(-1);
        if (!first) {
          event.preventDefault();
          event.currentTarget.focus();
          return;
        }
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last?.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
    >
      <div className="flex items-start justify-between gap-3 border-b border-border px-5 py-4">
        <div>
          <h2 id={id} className="text-lg font-semibold">
            {title}
          </h2>
          {description && (
            <p id={id + "-description"} className="mt-1 subtle">
              {description}
            </p>
          )}
        </div>
        <button
          type="button"
          aria-label="Fechar"
          disabled={busy}
          className="flex size-10 shrink-0 items-center justify-center rounded-lg hover:bg-muted"
          onClick={onClose}
        >
          <X size={18} aria-hidden />
        </button>
      </div>
      <div className="max-h-[65dvh] overflow-y-auto p-5">{children}</div>
    </dialog>
  );
}
