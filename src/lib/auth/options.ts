import type { BetterAuthOptions } from "better-auth";
import { z } from "zod";

export function authOptions() {
  const parsed = z.object({
    BETTER_AUTH_SECRET: z.string().min(32),
    BETTER_AUTH_URL: z.url(),
  }).safeParse(process.env);
  if (!parsed.success) throw new Error("Configure BETTER_AUTH_SECRET e BETTER_AUTH_URL no ambiente local.");
  return {
    appName: "TS Workspace",
    baseURL: parsed.data.BETTER_AUTH_URL,
    secret: parsed.data.BETTER_AUTH_SECRET,
    trustedOrigins: [new URL(parsed.data.BETTER_AUTH_URL).origin],
    emailAndPassword: {
      enabled: true, disableSignUp: true, autoSignIn: false,
      minPasswordLength: 12, maxPasswordLength: 128,
    },
    session: { cookieCache: { enabled: false }, expiresIn: 60 * 60 * 24 * 7, updateAge: 60 * 60 * 24 },
    advanced: { database: { generateId: "uuid" }, disableOriginCheck: false, disableCSRFCheck: false },
    rateLimit: { enabled: true, window: 60, max: 60, customRules: { "/sign-in/email": { window: 60, max: 5 } } },
    logger: { disabled: true },
    telemetry: { enabled: false },
  } satisfies BetterAuthOptions;
}
