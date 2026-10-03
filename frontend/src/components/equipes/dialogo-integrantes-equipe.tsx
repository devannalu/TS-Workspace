"use client";
import { useState, useTransition, useEffect } from "react";
import { adicionarIntegranteComFeedback, removerIntegranteComFeedback } from "@/lib/api/acoes";
import { buscarEquipe, type DetalheEquipe } from "@/lib/api/equipes";
import { listarUsuarios } from "@/lib/api/usuarios";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Dialog } from "../ui/dialog";
import type { EquipeResumo } from "./dialogos-equipe";

export function DialogoIntegrantesEquipe({ aberto, equipeSelecionada, usuarios, onClose, onSaved }: {
  aberto: boolean;
  equipeSelecionada: EquipeResumo | null;
  usuarios: DetalheEquipe["members"];
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const [salvando, iniciarTransicao] = useTransition();
  const [mensagemErro, definirMensagemErro] = useState("");
  const [usuariosDisponiveis, definirUsuariosDisponiveis] = useState(usuarios);
  const [integrantes, definirIntegrantes] = useState<DetalheEquipe["members"]>([]);
  const [carregandoIntegrantes, definirCarregandoIntegrantes] = useState(aberto);
  useEffect(() => {
    if (!aberto || !equipeSelecionada) return;
    let componenteAtivo = true;
    buscarEquipe(equipeSelecionada.id)
      .then((detalheEquipe) => {
        if (componenteAtivo) definirIntegrantes(detalheEquipe.members);
      })
      .catch((erro) => {
        if (componenteAtivo)
          definirMensagemErro(
            erro instanceof Error
              ? erro.message
              : "Não foi possível carregar integrantes.",
          );
      })
      .finally(() => {
        if (componenteAtivo) definirCarregandoIntegrantes(false);
      });
    return () => {
      componenteAtivo = false;
    };
  }, [aberto, equipeSelecionada]);
  const equipeAtual = equipeSelecionada ? { ...equipeSelecionada, members: integrantes } : null;
  const fecharDialogo = () => {
    if (!salvando) onClose();
  };
  const salvarAlteracaoEquipe = (
    mutacao: () => Promise<{ ok: boolean; error?: string }>,
    mensagemSucesso: string,
  ) =>
    iniciarTransicao(async () => {
      definirMensagemErro("");
      const resultadoAlteracao = await mutacao();
      if (!resultadoAlteracao.ok) {
        definirMensagemErro(resultadoAlteracao.error ?? "Não foi possível salvar a alteração.");
        return;
      }
      if (equipeSelecionada) {
        try {
          const detail = await buscarEquipe(equipeSelecionada.id);
          definirIntegrantes(detail.members);
        } catch (erroAtualizacao) {
          definirMensagemErro(
            erroAtualizacao instanceof Error
              ? erroAtualizacao.message
              : "Atualização salva, mas não foi possível recarregar integrantes.",
          );
        }
      } else onClose();
      onSaved(mensagemSucesso);
    });
  return (
      <Dialog
        open={aberto}
        onClose={fecharDialogo}
        busy={salvando}
        title={`Integrantes · ${equipeAtual?.name ?? ""}`}
        description="Organize quem participa desta equipe."
      >
        {carregandoIntegrantes ? (
          <p role="status" className="subtle">
            Carregando integrantes…
          </p>
        ) : (
          equipeAtual && (
            <div className="space-y-4">
              <ul className="space-y-2">
                {equipeAtual.members.map((user) => (
                  <li
                    key={user.id}
                    className="flex items-center justify-between gap-2 rounded-xl border border-border p-3"
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-medium">{user.name}</p>
                      <p className="break-all text-xs text-muted-foreground">
                        {user.email}
                      </p>
                    </div>
                    <Button
                      variant="ghost"
                      disabled={salvando}
                      aria-label={`Remover ${user.name}`}
                      onClick={() =>
                        salvarAlteracaoEquipe(
                          () => removerIntegranteComFeedback(user.id, equipeAtual.id),
                          "Integrante removida.",
                        )
                      }
                    >
                      Remover
                    </Button>
                  </li>
                ))}
              </ul>
              {!equipeAtual.members.length && (
                <p className="subtle">Esta equipe ainda não tem integrantes.</p>
              )}
              <form
                className="flex gap-2"
                onSubmit={(event) => {
                  event.preventDefault();
                  const busca = String(
                    new FormData(event.currentTarget).get("search"),
                  );
                  iniciarTransicao(async () => {
                    try {
                      const paginaUsuarios = await listarUsuarios({
                        status: "ACTIVE",
                        size: 25,
                        search: busca,
                      });
                      definirUsuariosDisponiveis(paginaUsuarios.items);
                      definirMensagemErro("");
                    } catch (erroBusca) {
                      definirMensagemErro(
                        erroBusca instanceof Error
                          ? erroBusca.message
                          : "Não foi possível buscar integrantes.",
                      );
                    }
                  });
                }}
              >
                <label className="field flex-1">
                  Buscar integrante
                  <Input
                    name="search"
                    placeholder="Nome ou e-mail"
                    disabled={salvando}
                  />
                </label>
                <Button
                  variant="secondary"
                  type="submit"
                  disabled={salvando}
                  className="self-end"
                >
                  Buscar
                </Button>
              </form>
              <form
                className="space-y-3"
                onSubmit={(event) => {
                  event.preventDefault();
                  const dadosFormulario = new FormData(event.currentTarget);
                  salvarAlteracaoEquipe(
                    () =>
                      adicionarIntegranteComFeedback(
                        String(dadosFormulario.get("userId")),
                        equipeAtual.id,
                      ),
                    "Integrante adicionada.",
                  );
                }}
              >
                <label className="field">
                  Adicionar integrante
                  <select name="userId" required disabled={salvando}>
                    <option value="">Selecione uma usuária ativa</option>
                    {usuariosDisponiveis
                      .filter(
                        (u) =>
                          !equipeAtual.members.some((member) => member.id === u.id),
                      )
                      .map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.name}
                        </option>
                      ))}
                  </select>
                </label>
                <Button type="submit" disabled={salvando}>
                  Adicionar
                </Button>
              </form>
              {mensagemErro && (
                <p role="alert" className="feedback-error">
                  {mensagemErro}
                </p>
              )}
            </div>
          )
        )}
      </Dialog>
  );
}
