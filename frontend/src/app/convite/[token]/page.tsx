import { ShellPublico } from "@/components/layout/shell-publico";
import { ConvitePublico } from "@/components/convites/convite-publico";
export const metadata = { title: "Aceitar convite", referrer: "no-referrer" };
export default async function InvitePage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = await params;
  return (
    <ShellPublico>
      <ConvitePublico key={token} token={token} />
    </ShellPublico>
  );
}
