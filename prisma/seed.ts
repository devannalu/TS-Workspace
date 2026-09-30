import { config } from "dotenv";
import { createDatabaseClient } from "../src/lib/db/client";
import { seedCore } from "./seed-data";

config({ quiet: true });
const db = createDatabaseClient(process.env.DATABASE_URL ?? "");
try { console.log("Seed concluído:", await seedCore(db)); }
catch { console.error("Seed falhou; confira conexão e estado do schema. Valores sensíveis foram omitidos."); process.exitCode = 1; }
finally { await db.$disconnect(); }
