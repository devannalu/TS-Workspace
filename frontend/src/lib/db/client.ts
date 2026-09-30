import { PrismaMariaDb } from "@prisma/adapter-mariadb";
import { PrismaClient } from "../../generated/prisma/client";

export function createDatabaseClient(databaseUrl: string) {
  let url: URL;
  try { url = new URL(databaseUrl); } catch { throw new Error("DATABASE_URL inválida."); }
  if (url.protocol !== "mysql:") throw new Error("O provider deve ser MySQL.");
  const adapter = new PrismaMariaDb({
    host: url.hostname, port: Number(url.port || 3306),
    user: decodeURIComponent(url.username), password: decodeURIComponent(url.password),
    database: url.pathname.slice(1), connectionLimit: 5,
    timezone: "Z",
  });
  return new PrismaClient({ adapter, log: [] });
}
