export type TeamNode = { id: string; parentId: string | null; archivedAt: Date | null };

export function validateTeamParent(teams: readonly TeamNode[], teamId: string, parentId: string | null) {
  if (parentId === null) throw new Error("Não é permitido criar outra raiz pelo fluxo administrativo.");
  if (teamId === parentId) throw new Error("Uma equipe não pode ser sua própria equipe mãe.");
  const byId = new Map(teams.map(team => [team.id, team]));
  const current = byId.get(teamId);
  if (current?.parentId === null) throw new Error("A equipe raiz não pode ser movida.");
  if (current?.archivedAt) throw new Error("Uma equipe arquivada não pode ser movida.");
  const parent = byId.get(parentId);
  if (!parent) throw new Error("Equipe mãe inexistente.");
  if (parent.archivedAt) throw new Error("Equipe mãe arquivada.");
  const visited = new Set<string>([teamId]);
  let cursor: TeamNode | undefined = parent;
  while (cursor) {
    if (visited.has(cursor.id)) throw new Error("A alteração criaria ou manteria um ciclo na hierarquia.");
    visited.add(cursor.id);
    if (cursor.parentId === null) break;
    cursor = byId.get(cursor.parentId);
    if (!cursor) throw new Error("Hierarquia inválida: ancestral inexistente.");
  }
}
