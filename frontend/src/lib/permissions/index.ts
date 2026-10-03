import "server-only";
import { requireAuth } from "../auth/session";
export async function hasPermission(key: string) { return (await requireAuth()).permissions.includes(key); }
