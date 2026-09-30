import "server-only";
import { cache } from "react";
import { headers } from "next/headers";
import { redirect } from "next/navigation";
import { auth } from "./index";
import { db } from "../db";
import { findCurrentUser } from "./current-user";

export const getCurrentUser = cache(async () => findCurrentUser(db, auth, await headers()));
export async function requireAuth() {
  const user = await getCurrentUser();
  if (!user) redirect("/login");
  return user;
}
