import { spawnSync } from "node:child_process";
import { config } from "dotenv";
import { requireTestDatabase } from "../src/lib/db/test-safety";

config({ quiet: true });
requireTestDatabase(process.env.TEST_DATABASE_URL, process.env.DATABASE_URL);
const migration = spawnSync(process.execPath, ["node_modules/prisma/build/index.js", "migrate", "deploy", "--config", "prisma.test.config.ts"], { stdio: "inherit" });
if (migration.status !== 0) process.exit(migration.status ?? 1);
const tests = spawnSync(process.execPath, ["node_modules/vitest/vitest.mjs", "run", "tests/integration"], { stdio: "inherit" });
process.exitCode = tests.status ?? 1;
