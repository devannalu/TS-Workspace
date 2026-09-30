import { config } from "dotenv";
import { betterAuth } from "better-auth";
import { authOptions } from "../src/lib/auth/options";

config({ quiet: true });
export const auth = betterAuth(authOptions());
