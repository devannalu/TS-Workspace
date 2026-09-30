import { config } from "dotenv";
import { createDatabaseClient } from "../src/lib/db/client";
import { createPublicAuth } from "../src/lib/auth/factory";

config({ quiet: true });
export const auth = createPublicAuth(createDatabaseClient(process.env.DATABASE_URL ?? ""));
