import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { PublicInvite } from "@/components/public-invite";
export const metadata={title:"Aceitar convite",referrer:"no-referrer"};
export default async function InvitePage({params}:{params:Promise<{token:string}>}) {
 const {token}=await params;
 return <WorkspaceShell><div className="mx-auto w-full max-w-xl"><Card className="p-6 sm:p-8"><Badge>Tech Sisters</Badge><PublicInvite key={token} token={token}/></Card></div></WorkspaceShell>;
}
