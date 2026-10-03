import { createJavaTeam, editJavaTeam, archiveJavaTeam, addJavaTeamMember, removeJavaTeamMember } from "./teams";
import { createJavaInvite, cancelJavaInvite, acceptJavaInvite } from "./invites";
async function perform(work:()=>Promise<unknown>) {
 try { await work(); return {ok:true as const}; }
 catch(error) { return {ok:false as const,error:error instanceof Error?error.message:"Não foi possível concluir a operação."}; }
}
export const createTeamAction=(input:{name:string;description:string;parentId:string})=>perform(()=>createJavaTeam(input));
export const updateTeamAction=(input:{teamId:string;name:string;description:string;parentId:string})=>perform(()=>editJavaTeam(input.teamId,input));
export const archiveTeamAction=(id:string)=>perform(()=>archiveJavaTeam(id));
export const addTeamMemberAction=(userId:string,teamId:string)=>perform(()=>addJavaTeamMember(teamId,userId));
export const removeTeamMemberAction=(userId:string,teamId:string)=>perform(()=>removeJavaTeamMember(teamId,userId));
export const cancelInviteAction=(id:string)=>perform(()=>cancelJavaInvite(id));
export const acceptInviteAction=(input:{token:string;name:string;password:string;passwordConfirmation:string})=>perform(()=>acceptJavaInvite(input));
export async function createInviteAction(input:{email:string;roleId:string;teamIds:string[]}) {
 try { const result=await createJavaInvite(input); return {ok:true as const,url:result.inviteUrl}; }
 catch(error) { return {ok:false as const,error:error instanceof Error?error.message:"Não foi possível criar o convite."}; }
}
