import type { PrismaClient } from "../src/generated/prisma/client";
import { permissionKeys } from "../src/lib/permissions/policy";

const roles = [
  { key: "SUPER_ADMIN", name: "Super Admin", permissions: [...permissionKeys] },
  { key: "ADMIN", name: "Admin", permissions: permissionKeys.filter(key => key !== "permissions.manage") },
  { key: "SUPERVISOR", name: "Supervisora", permissions: ["users.view", "teams.view", "settings.view"] },
  { key: "SUPPORT", name: "Suporte", permissions: ["teams.view", "settings.view"] },
];
const teams = [
  { key: "comunicacao", name: "Comunicação" },
  { key: "eventos", name: "Eventos" },
  { key: "desenvolvimento-projetos", name: "Desenvolvimento de Projetos" },
  { key: "comunicacao-interna", name: "Comunicação Interna" },
];

export async function seedCore(db: PrismaClient) {
  await db.$transaction(async tx => {
    for (const key of permissionKeys) {
      await tx.permission.upsert({ where: { key }, update: {}, create: { key, name: key } });
    }
    for (const definition of roles) {
      const role = await tx.role.upsert({ where: { key: definition.key }, update: {}, create: { key: definition.key, name: definition.name } });
      // Reruns apenas completam grants padrão novos; nunca removem decisões administrativas.
      for (const key of definition.permissions) {
        const permission = await tx.permission.findUniqueOrThrow({ where: { key } });
        if (!await tx.rolePermission.findUnique({ where: { roleId_permissionId: { roleId: role.id, permissionId: permission.id } } })) {
          await tx.rolePermission.create({ data: { roleId: role.id, permissionId: permission.id } });
        }
      }
    }
    const root = await tx.team.upsert({ where: { key: "fundadoras" }, update: {}, create: { key: "fundadoras", name: "Fundadoras" } });
    if (root.parentId !== null || root.archivedAt) throw new Error("Equipe Fundadoras em estado inválido; seed cancelado.");
    for (const team of teams) await tx.team.upsert({ where: { key: team.key }, update: {}, create: { ...team, parentId: root.id } });
  });
  return { roles: await db.role.count(), permissions: await db.permission.count(), rolePermissions: await db.rolePermission.count(), teams: await db.team.count() };
}
