import type { UsuarioAtual } from "../api/autenticacao";
import type { PaginaApi } from "../api/contratos";
import { acessosPainel } from "./permissoes";
export async function buscarTotaisPainel(
  usuario: UsuarioAtual,
  read: <T>(path: string) => Promise<T>,
) {
  const acessos = acessosPainel(usuario);
  const users = acessos.podeVerUsuarios
    ? read<PaginaApi<unknown>>("/users?status=ACTIVE&size=1").then(
        (page) => page.total,
      )
    : Promise.resolve(null);
  const pending = acessos.podeVerUsuarios
    ? read<number>("/invites/pending-count")
    : Promise.resolve(null);
  const [usuariosAtivos, convitesPendentes] = await Promise.all([users, pending]);
  return { usuariosAtivos, convitesPendentes };
}
