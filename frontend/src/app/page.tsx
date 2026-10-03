import { redirect } from "next/navigation";
import { buscarUsuarioDaSessao } from "@/lib/sessao";

export default async function Home() {
  redirect(await buscarUsuarioDaSessao() ? "/workspace" : "/login");
}
