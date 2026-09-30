import "server-only";
import { createDatabaseClient } from "./client";

const databaseGlobal = globalThis as unknown as { tsWorkspaceDb?: ReturnType<typeof createDatabaseClient> };
if (!process.env.DATABASE_URL) throw new Error("Configure DATABASE_URL.");
export const db = databaseGlobal.tsWorkspaceDb ?? createDatabaseClient(process.env.DATABASE_URL);
if (process.env.NODE_ENV !== "production") databaseGlobal.tsWorkspaceDb = db;
