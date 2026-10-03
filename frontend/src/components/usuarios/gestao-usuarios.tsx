"use client";
import { useState, useTransition, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Search } from "lucide-react";
import type { UsuarioGerenciado } from "@/lib/api/usuarios";
import { ListaUsuarios } from "./lista-usuarios";
import { DialogosUsuario } from "./dialogos-usuario";
import type { PaginaApi, PerfilAcessoReferencia, EquipeReferencia } from "@/lib/api/contratos";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Pagination } from "../ui/pagination";
import { Toast } from "../ui/feedback";
export function GestaoUsuarios({
  paginaUsuarios,
  perfisAcesso,
  equipes,
  podeEditar,
  podeInativar,
}: {
  paginaUsuarios: PaginaApi<UsuarioGerenciado>;
  perfisAcesso: PerfilAcessoReferencia[];
  equipes: EquipeReferencia[];
  podeEditar: boolean;
  podeInativar: boolean;
}) {
  const router = useRouter(),
    parametrosBusca = useSearchParams(),
    [salvando, iniciarTransicao] = useTransition(),
    [usuarioSelecionado, definirUsuarioSelecionado] = useState<UsuarioGerenciado | null>(null),
    [modo, definirModo] = useState<"edit" | "status" | null>(null),
    [mensagemSucesso, definirMensagemSucesso] = useState(""),
    [mensagemErro, definirMensagemErro] = useState("");
  const fecharFeedback = useCallback(() => definirMensagemSucesso(""), []);
  const atualizarFiltros = (novosFiltros: Record<string, string>) => {
    const proximosParametros = new URLSearchParams(parametrosBusca.toString());
    for (const [nomeFiltro, valorFiltro] of Object.entries(novosFiltros)) {
      if (valorFiltro) proximosParametros.set(nomeFiltro, valorFiltro);
      else proximosParametros.delete(nomeFiltro);
    }
    router.push("/usuarias?" + proximosParametros);
  };
  const fecharDialogo = () => {
    if (!salvando) {
      definirUsuarioSelecionado(null);
      definirModo(null);
      definirMensagemErro("");
    }
  };
  const atualizarUsuarioERecarregar = (
    mutacao: () => Promise<unknown>,
    mensagemSucesso: string,
  ) =>
    iniciarTransicao(async () => {
      try {
        definirMensagemErro("");
        await mutacao();
        definirUsuarioSelecionado(null);
        definirModo(null);
        definirMensagemSucesso(mensagemSucesso);
        router.refresh();
      } catch (erro) {
        definirMensagemErro(
          erro instanceof Error
            ? erro.message
            : "Não foi possível concluir a operação.",
        );
      }
    });
  const escolherAcaoUsuario = (usuario: UsuarioGerenciado, acao: typeof modo) => {
    definirMensagemErro("");
    definirUsuarioSelecionado(usuario);
    definirModo(acao);
  };
  return (
    <div className="space-y-5">
      <form
        key={parametrosBusca.toString()}
        className="grid gap-3 rounded-xl bg-muted p-4 sm:grid-cols-2 xl:grid-cols-[1.6fr_1fr_1fr_1fr_auto]"
        aria-label="Filtros de usuárias"
        onSubmit={(event) => {
          event.preventDefault();
          const dadosFormulario = new FormData(event.currentTarget);
          atualizarFiltros({
            search: String(dadosFormulario.get("search")).trim(),
            status: String(dadosFormulario.get("status")),
            roleId: String(dadosFormulario.get("roleId")),
            teamId: String(dadosFormulario.get("teamId")),
            page: "0",
          });
        }}
      >
        <label className="field">
          Busca
          <Input
            name="search"
            defaultValue={parametrosBusca.get("search") ?? ""}
            placeholder="Nome ou e-mail"
            maxLength={120}
          />
        </label>
        <label className="field">
          Status
          <select name="status" defaultValue={parametrosBusca.get("status") ?? ""}>
            <option value="">Todos</option>
            <option value="ACTIVE">Ativa</option>
            <option value="INACTIVE">Inativa</option>
          </select>
        </label>
        <label className="field">
          Perfil
          <select name="roleId" defaultValue={parametrosBusca.get("roleId") ?? ""}>
            <option value="">Todos</option>
            {perfisAcesso.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          Equipe
          <select name="teamId" defaultValue={parametrosBusca.get("teamId") ?? ""}>
            <option value="">Todas</option>
            {equipes.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </label>
        <div className="flex items-end gap-2">
          <Button type="submit" aria-label="Aplicar filtros">
            <Search size={17} aria-hidden />
          </Button>
          <Button
            variant="ghost"
            onClick={() =>
              atualizarFiltros({
                search: "",
                status: "",
                roleId: "",
                teamId: "",
                page: "0",
              })
            }
          >
            Limpar
          </Button>
        </div>
      </form>
      <ListaUsuarios
        paginaUsuarios={paginaUsuarios}
        podeEditar={podeEditar}
        podeInativar={podeInativar}
        escolherAcaoUsuario={escolherAcaoUsuario}
      />
      <Pagination
        page={paginaUsuarios.page}
        size={paginaUsuarios.size}
        total={paginaUsuarios.total}
        label="usuárias"
        onPage={(page) => atualizarFiltros({ page: String(page) })}
      />
      <DialogosUsuario
        usuarioSelecionado={usuarioSelecionado}
        modo={modo}
        perfisAcesso={perfisAcesso}
        equipes={equipes}
        salvando={salvando}
        mensagemErro={mensagemErro}
        fecharDialogo={fecharDialogo}
        atualizarUsuarioERecarregar={atualizarUsuarioERecarregar}
      />
      <Toast message={mensagemSucesso} onClose={fecharFeedback} />
    </div>
  );
}
