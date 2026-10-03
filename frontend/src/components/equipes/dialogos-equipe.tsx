"use client";
import { useState, useTransition } from "react";
import {
  criarEquipeComFeedback,
  editarEquipeComFeedback,
  arquivarEquipeComFeedback,
} from "@/lib/api/acoes";
import {
  type Equipe,
  type DetalheEquipe,
} from "@/lib/api/equipes";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Dialog } from "../ui/dialog";
import { DialogoIntegrantesEquipe } from "./dialogo-integrantes-equipe";
export type EquipeResumo = Pick<
  Equipe,
  "id" | "name" | "description" | "parentId" | "memberCount"
>;
export type UsuarioEquipe = DetalheEquipe["members"][number];
export function DialogosEquipe({
  modo,
  equipeSelecionada,
  equipes,
  usuarios,
  onClose,
  onSaved,
}: {
  modo: "create" | "edit" | "members" | "archive" | null;
  equipeSelecionada: EquipeResumo | null;
  equipes: EquipeResumo[];
  usuarios: UsuarioEquipe[];
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const [salvando, iniciarTransicao] = useTransition();
  const [mensagemErro, definirMensagemErro] = useState("");
  const equipeAtual = equipeSelecionada;
  const equipeRaiz = equipes.find((team) => !team.parentId);
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
      onClose();
      onSaved(mensagemSucesso);
    });
  return (
    <>
      <Dialog
        open={modo === "create" || modo === "edit"}
        onClose={fecharDialogo}
        busy={salvando}
        title={modo === "edit" ? "Editar equipe" : "Nova equipe"}
        description="Defina o nome e a posição da equipe na organização."
      >
        {(modo === "create" || equipeAtual) && (
          <form
            key={equipeAtual?.id ?? "create"}
            className="space-y-4"
            aria-busy={salvando}
            onSubmit={(event) => {
              event.preventDefault();
              const dadosFormulario = new FormData(event.currentTarget),
                dadosEquipe = {
                  name: String(dadosFormulario.get("name")).trim(),
                  description: String(dadosFormulario.get("description")),
                  parentId: String(dadosFormulario.get("parentId")),
                };
              salvarAlteracaoEquipe(
                () =>
                  modo === "edit" && equipeAtual
                    ? editarEquipeComFeedback({ teamId: equipeAtual.id, ...dadosEquipe })
                    : criarEquipeComFeedback(dadosEquipe),
                modo === "edit" ? "Equipe atualizada." : "Equipe criada.",
              );
            }}
          >
            <label className="field">
              Nome
              <Input
                name="name"
                defaultValue={modo === "edit" ? equipeAtual?.name : ""}
                required
                maxLength={100}
                minLength={2}
                disabled={salvando}
              />
            </label>
            <label className="field">
              Descrição
              <Input
                name="description"
                defaultValue={
                  modo === "edit" ? (equipeAtual?.description ?? "") : ""
                }
                maxLength={500}
                disabled={salvando}
              />
            </label>
            <label className="field">
              Equipe superior
              <select
                name="parentId"
                defaultValue={
                  modo === "edit" ? (equipeAtual?.parentId ?? equipeRaiz?.id) : equipeRaiz?.id
                }
                required
                disabled={salvando}
              >
                {equipes
                  .filter((t) => t.id !== equipeAtual?.id)
                  .map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
              </select>
            </label>
            {mensagemErro && (
              <p role="alert" className="feedback-error">
                {mensagemErro}
              </p>
            )}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={fecharDialogo} disabled={salvando}>
                Cancelar
              </Button>
              <Button type="submit" disabled={salvando}>
                {salvando ? "Salvando…" : "Salvar equipe"}
              </Button>
            </div>
          </form>
        )}
      </Dialog>
      <Dialog
        open={modo === "archive"}
        onClose={fecharDialogo}
        busy={salvando}
        title="Arquivar equipe"
        description="A equipe será arquivada e não poderá ser usada como equipe superior. Resolva as equipes filhas ativas primeiro."
      >
        <p className="font-medium">{equipeAtual?.name}</p>
        {mensagemErro && (
          <p role="alert" className="mt-3 feedback-error">
            {mensagemErro}
          </p>
        )}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={fecharDialogo} disabled={salvando}>
            Voltar
          </Button>
          <Button
            variant="danger"
            disabled={salvando}
            onClick={() =>
              equipeAtual &&
              salvarAlteracaoEquipe(
                () => arquivarEquipeComFeedback(equipeAtual.id),
                "Equipe arquivada.",
              )
            }
          >
            {salvando ? "Arquivando…" : "Confirmar arquivamento"}
          </Button>
        </div>
      </Dialog>
      <DialogoIntegrantesEquipe
        aberto={modo === "members"}
        equipeSelecionada={equipeSelecionada}
        usuarios={usuarios}
        onClose={onClose}
        onSaved={onSaved}
      />
    </>
  );
}
