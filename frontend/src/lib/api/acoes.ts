import { criarEquipe, editarEquipe, arquivarEquipe, adicionarIntegranteEquipe, removerIntegranteEquipe, type SalvarEquipeRequest } from "./equipes";
import { criarConvite, cancelarConvite, aceitarConvite, type CriarConviteRequest, type AceitarConviteRequest } from "./convites";
async function capturarResultadoMutacao(mutacao: () => Promise<unknown>) {
  try {
    await mutacao();
    return { ok: true as const };
  }
  catch (erro) {
    return {
      ok: false as const,
      error: erro instanceof Error ? erro.message : "Não foi possível concluir a operação.",
    };
  }
}
export const criarEquipeComFeedback = (dadosEquipe: SalvarEquipeRequest) => capturarResultadoMutacao(() => criarEquipe(dadosEquipe));
export const editarEquipeComFeedback = (dadosEquipe: SalvarEquipeRequest & {
  teamId: string;
}) => capturarResultadoMutacao(() => editarEquipe(dadosEquipe.teamId, dadosEquipe));
export const arquivarEquipeComFeedback = (equipeId: string) => capturarResultadoMutacao(() => arquivarEquipe(equipeId));
export const adicionarIntegranteComFeedback = (usuarioId: string, equipeId: string) => capturarResultadoMutacao(() => adicionarIntegranteEquipe(equipeId, usuarioId));
export const removerIntegranteComFeedback = (usuarioId: string, equipeId: string) => capturarResultadoMutacao(() => removerIntegranteEquipe(equipeId, usuarioId));
export const cancelarConviteComFeedback = (conviteId: string) => capturarResultadoMutacao(() => cancelarConvite(conviteId));
export const aceitarConviteComFeedback = (dadosAceite: AceitarConviteRequest) => capturarResultadoMutacao(() => aceitarConvite(dadosAceite));
export async function criarConviteComFeedback(dadosConvite: CriarConviteRequest) {
  try {
    const conviteCriado = await criarConvite(dadosConvite);
    return { ok: true as const, url: conviteCriado.inviteUrl };
  }
  catch (erro) {
    return {
      ok: false as const,
      error: erro instanceof Error ? erro.message : "Não foi possível criar o convite.",
    };
  }
}
