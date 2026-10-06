package com.devannalu.tsworkspace.projetos;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;

class PoliticaProjetoTest {
    @Test void deveValidarTituloETexto() {
        assertThat(PoliticaProjeto.validarTitulo("  Projeto  ")).isEqualTo("Projeto");
        assertThatThrownBy(()->PoliticaProjeto.validarTitulo(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->PoliticaProjeto.validarTitulo("x".repeat(201))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->PoliticaProjeto.validarDescricao("x".repeat(5001))).isInstanceOf(IllegalArgumentException.class);
        assertThat(PoliticaProjeto.validarDescricao(" ")).isNull();
    }
    @Test void deveAceitarDatasOpcionaisERejeitarPeriodoInvertido() {
        var hoje=LocalDate.now();PoliticaProjeto.validarPeriodo(null,hoje);PoliticaProjeto.validarPeriodo(hoje,null);
        PoliticaProjeto.validarPeriodo(hoje,hoje);
        assertThatThrownBy(()->PoliticaProjeto.validarPeriodo(hoje.plusDays(1),hoje)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deveDistinguirSemTarefasDeZeroPorCento() {
        assertThat(PoliticaProjeto.percentual(0,0)).isNull();assertThat(PoliticaProjeto.percentual(2,0)).isZero();
        assertThat(PoliticaProjeto.percentual(3,2)).isEqualTo(67);assertThat(PoliticaProjeto.percentual(2,2)).isEqualTo(100);
    }
    @Test void deveExigirVersaoAtual() {
        assertThatThrownBy(()->PoliticaProjeto.exigirVersao(2,1)).isInstanceOf(ProblemaDominio.class)
            .hasMessage("Este projeto foi atualizado por outra pessoa. Atualize os dados e tente novamente.");
        assertThatThrownBy(()->PoliticaProjeto.exigirVersao(0,-1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deveDistinguirConflitosDeConclusaoEArquivo() {
        PoliticaProjeto.exigirSemPendencias(0,0,false);PoliticaProjeto.exigirSemPendencias(2,2,true);
        assertThatThrownBy(()->PoliticaProjeto.exigirSemPendencias(2,1,false)).hasMessageContaining("concluir o projeto");
        assertThatThrownBy(()->PoliticaProjeto.exigirSemPendencias(2,1,true)).hasMessageContaining("arquivar o projeto");
    }
    @Test void deveManterEscopoMesmoSemResponsabilidade() {
        var suporte=new PoliticaProjeto.Acesso("id","SUPPORT",Set.of("projects.view"));
        assertThatThrownBy(()->PoliticaProjeto.exigirEquipe(suporte,false)).isInstanceOf(AccessDeniedException.class);
        PoliticaProjeto.exigirEquipe(new PoliticaProjeto.Acesso("id","ADMIN",Set.of()),false);
    }
}
