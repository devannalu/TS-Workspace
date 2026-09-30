import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/auth/session";

export default async function Home() {
  redirect(await getCurrentUser() ? "/workspace" : "/login");
}
