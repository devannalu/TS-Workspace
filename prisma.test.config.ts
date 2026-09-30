import { config } from "dotenv";
import { defineConfig } from "prisma/config";
import { requireTestDatabase } from "./src/lib/db/test-safety";

config({ quiet: true });
export default defineConfig({
  schema: "prisma/schema.prisma",
  migrations: { path: "prisma/migrations" },
  datasource: { url: requireTestDatabase(process.env.TEST_DATABASE_URL) },
});
