"use client";
import { useEffect } from "react";
import { X, Inbox, AlertCircle } from "lucide-react";
import { Button } from "./button";
export function Toast({
  message,
  onClose,
}: {
  message: string;
  onClose: () => void;
}) {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(onClose, 4500);
    return () => clearTimeout(timer);
  }, [message, onClose]);
  if (!message) return null;
  return (
    <div
      role="status"
      aria-live="polite"
      className="toast flex items-center gap-4"
    >
      {message}
      <button
        type="button"
        aria-label="Dispensar mensagem"
        className="flex size-11 shrink-0 items-center justify-center rounded-lg"
        onClick={onClose}
      >
        <X size={16} aria-hidden />
      </button>
    </div>
  );
}
export function EmptyState({
  title,
  description,
}: {
  title: string;
  description: string;
}) {
  return (
    <div className="rounded-xl border border-dashed border-border p-8 text-center">
      <Inbox
        size={24}
        aria-hidden
        className="mx-auto mb-3 text-muted-foreground"
      />
      <h3 className="font-medium">{title}</h3>
      <p className="mt-2 subtle">{description}</p>
    </div>
  );
}
export function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry?: () => void;
}) {
  return (
    <div role="alert" className="feedback-error">
      <div className="flex items-center gap-2">
        <AlertCircle size={18} aria-hidden />
        {message}
      </div>
      {onRetry && (
        <Button variant="secondary" className="mt-3" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </div>
  );
}
export function Skeleton() {
  return (
    <div role="status" aria-label="Carregando" className="space-y-4 p-5">
      <span className="sr-only">Carregando…</span>
      <div className="h-8 w-48 animate-pulse rounded-lg bg-muted" />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-28 animate-pulse rounded-xl bg-muted" />
        ))}
      </div>
      <div className="h-56 animate-pulse rounded-xl bg-muted" />
    </div>
  );
}
