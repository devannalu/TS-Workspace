import { PublicShell } from "@/components/layout/public-shell";
import { PublicInvite } from "@/components/public-invite";
export const metadata = { title: "Aceitar convite", referrer: "no-referrer" };
export default async function InvitePage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  return (
    <PublicShell>
      <PublicInvite key={token} token={token} />
    </PublicShell>
  );
}
