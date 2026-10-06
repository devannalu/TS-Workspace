package com.devannalu.tsworkspace.tarefas;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;

class PoliticaTarefaTest {
    @Test void deveExigirTituloCurtoEAplicarTrim() {
        assertThat(PoliticaTarefa.validarTitulo("  Planejar encontro  ")).isEqualTo("Planejar encontro");
        for (String titulo : new String[]{"", " ", "x".repeat(201)})
            assertThatThrownBy(() -> PoliticaTarefa.validarTitulo(titulo)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PoliticaTarefa.validarTitulo(null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deveLimitarDescricaoEAceitarAusencia() {
        assertThat(PoliticaTarefa.validarDescricao("  ")).isNull();
        assertThat(PoliticaTarefa.validarDescricao(" texto ")).isEqualTo("texto");
        assertThatThrownBy(() -> PoliticaTarefa.validarDescricao("x".repeat(5001))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deveUsarPrioridadeMediaPorPadrao() {
        assertThat(PoliticaTarefa.prioridadeInicial(null)).isEqualTo(Tarefa.Prioridade.MEDIA);
        assertThat(PoliticaTarefa.prioridadeInicial(Tarefa.Prioridade.URGENTE)).isEqualTo(Tarefa.Prioridade.URGENTE);
    }
    @Test void deveDerivarAtrasoSemContarHojeNemConcluidas() {
        LocalDate hoje = LocalDate.of(2026, 10, 6);
        assertThat(PoliticaTarefa.atrasada(hoje.minusDays(1), Tarefa.Status.EM_ANDAMENTO, hoje)).isTrue();
        assertThat(PoliticaTarefa.atrasada(hoje, Tarefa.Status.A_FAZER, hoje)).isFalse();
        assertThat(PoliticaTarefa.atrasada(null, Tarefa.Status.A_FAZER, hoje)).isFalse();
        assertThat(PoliticaTarefa.atrasada(hoje.minusDays(1), Tarefa.Status.CONCLUIDA, hoje)).isFalse();
    }
    @Test void deveRestringirEquipeSemConcederAcessoPorPermissionApenas() {
        var supervisora = new PoliticaTarefa.Acesso("a", "SUPERVISOR", Set.of("tasks.view"));
        assertThatThrownBy(() -> PoliticaTarefa.exigirEquipe(supervisora, false)).isInstanceOf(AccessDeniedException.class);
        assertThatCode(() -> PoliticaTarefa.exigirEquipe(supervisora, true)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaTarefa.exigirEquipe(new PoliticaTarefa.Acesso("a", "ADMIN", Set.of()), false)).doesNotThrowAnyException();
    }
    @Test void deveLimitarEdicaoDeSuporteACriadoraOuResponsavel() {
        var suporte = new PoliticaTarefa.Acesso("a", "SUPPORT", Set.of("tasks.edit"));
        assertThat(PoliticaTarefa.podeEditar(suporte, "a", false)).isTrue();
        assertThat(PoliticaTarefa.podeEditar(suporte, "b", true)).isTrue();
        assertThat(PoliticaTarefa.podeEditar(suporte, "b", false)).isFalse();
        assertThat(PoliticaTarefa.podeEditar(new PoliticaTarefa.Acesso("a", "SUPPORT", Set.of()), "a", true)).isFalse();
    }
    @Test void deveRejeitarEquipeArquivadaEEdicaoDesatualizada() {
        assertThatThrownBy(() -> PoliticaTarefa.exigirEquipeAtiva(true)).hasMessageContaining("somente leitura");
        assertThatThrownBy(() -> PoliticaTarefa.exigirVersao(2, 1L)).hasMessageContaining("atualizada por outra pessoa");
        assertThatCode(() -> PoliticaTarefa.exigirVersao(2, 2L)).doesNotThrowAnyException();
    }
}
