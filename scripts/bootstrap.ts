import { config } from "dotenv";
import { createDatabaseClient } from "../src/lib/db/client";
import { bootstrapFirstAdmin } from "../src/lib/auth/bootstrap";

config({ path: ".env.bootstrap.local", quiet: true });
config({ quiet: true });
const db = createDatabaseClient(process.env.DATABASE_URL ?? "");
try {
  const result = await bootstrapFirstAdmin(db, { name: process.env.BOOTSTRAP_NAME, email: process.env.BOOTSTRAP_EMAIL, password: process.env.BOOTSTRAP_PASSWORD });
  console.log(result.status === "created" ? "Primeira Super Admin criada e provisionada." : "Super Admin já provisionada; nenhum dado alterado.");
} catch {
  console.error("Bootstrap não concluído. Confira seed, dados locais e existência de conta ou estado parcial. Nenhum segredo foi registrado.");
  process.exitCode = 1;
} finally { await db.$disconnect(); }
