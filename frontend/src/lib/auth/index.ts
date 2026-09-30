import "server-only";
import { db } from "../db";
import { createPublicAuth } from "./factory";

export const auth = createPublicAuth(db);
