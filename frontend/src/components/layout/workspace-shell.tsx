import type { ReactNode } from "react";
import { requireAuth } from "@/lib/auth/session";
import { AppShell } from "./app-shell";
import { SessionBoundary } from "../session-boundary";
export async function WorkspaceShell({ children }: { children: ReactNode }) {
  const user = await requireAuth();
  return (
    <SessionBoundary>
      <AppShell user={user}>{children}</AppShell>
    </SessionBoundary>
  );
}
