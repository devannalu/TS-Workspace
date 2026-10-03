"use client";
import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { editJavaUser, deactivateJavaUser, activateJavaUser, type JavaManagedUser } from "@/lib/api/users";
import { ApiError } from "@/lib/api/http";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
type Option={id:string;name:string};
export function UserManagement({users,roles,teams,canEdit,canDisable}:{users:JavaManagedUser[];roles:Option[];teams:Option[];canEdit:boolean;canDisable:boolean}) {
 const router=useRouter(),[message,setMessage]=useState(""),[pending,start]=useTransition();
 const run=(work:()=>Promise<unknown>)=>start(async()=>{try{await work();setMessage("Usuária atualizada.");router.refresh();}catch(e){if(e instanceof ApiError&&e.status===401)router.replace("/login");setMessage(e instanceof Error?e.message:"Não foi possível concluir a operação.");}});
 return <div className="space-y-4">{users.map(user=><article key={user.id} className="rounded-2xl border border-border bg-card p-5">
 <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-semibold">{user.name}</h3><p className="text-sm text-muted-foreground">{user.email}</p><p className="text-sm text-muted-foreground">{user.jobTitle??"Sem cargo descritivo"} · {user.role.name}</p><p className="text-sm text-muted-foreground">{user.teams.map(t=>t.name).join(", ")||"Sem equipes ativas"}</p></div><span className="rounded-full bg-muted px-3 py-1 text-xs">{user.status==="ACTIVE"?"Ativa":"Inativa"}</span></div>
 {canEdit&&<form className="mt-5 grid gap-4 md:grid-cols-3" onSubmit={event=>{event.preventDefault();const form=new FormData(event.currentTarget);run(()=>editJavaUser(user.id,{jobTitle:String(form.get("jobTitle")),roleId:String(form.get("roleId")),teamIds:form.getAll("teamIds").map(String)}));}}>
 <label className="space-y-2 text-sm"><span>Cargo descritivo</span><Input name="jobTitle" defaultValue={user.jobTitle??""} maxLength={160} disabled={pending}/></label>
 <label className="space-y-2 text-sm"><span>Perfil de acesso</span><select name="roleId" defaultValue={user.role.id} className="min-h-11 w-full rounded-xl border border-border bg-card px-3" disabled={pending}>{roles.map(role=><option key={role.id} value={role.id}>{role.name}</option>)}</select></label>
 <fieldset className="space-y-2 text-sm"><legend>Equipes</legend><div className="flex flex-wrap gap-2">{teams.map(team=><label key={team.id} className="flex items-center gap-1"><input type="checkbox" name="teamIds" value={team.id} defaultChecked={user.teams.some(t=>t.id===team.id)} disabled={pending}/>{team.name}</label>)}</div></fieldset>
 <Button type="submit" disabled={pending} className="md:col-span-3 md:justify-self-start">{pending?"Salvando…":"Salvar alterações"}</Button></form>}
 {canDisable&&<Button type="button" className="mt-4 bg-muted text-foreground" disabled={pending} onClick={()=>run(()=>user.status==="ACTIVE"?deactivateJavaUser(user.id):activateJavaUser(user.id))}>{user.status==="ACTIVE"?"Inativar usuária":"Reativar usuária"}</Button>}
 </article>)}{!users.length&&<p className="rounded-2xl border border-dashed border-border p-8 text-center text-sm text-muted-foreground">Nenhuma usuária encontrada.</p>}{message&&<p role="status" className="text-sm">{message}</p>}</div>;
}
