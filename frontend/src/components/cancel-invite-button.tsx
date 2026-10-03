"use client";

import { useState, useTransition } from "react";
import { cancelInviteAction } from "@/lib/api/ui-actions";
import { Button } from "./ui/button";

export function CancelInviteButton({ inviteId }: { inviteId: string }) {
  const [pending, start] = useTransition(); const [error, setError] = useState("");
  return <span className="flex items-center gap-2">{error && <span role="alert" className="text-xs text-danger">{error}</span>}<Button type="button" disabled={pending} className="min-h-8 bg-muted px-3 py-1 text-xs text-foreground" onClick={() => { setError(""); start(async () => { const result = await cancelInviteAction(inviteId); if (!result.ok) setError(result.error); else window.location.reload(); }); }}>{pending ? "Cancelando…" : "Cancelar"}</Button></span>;
}
