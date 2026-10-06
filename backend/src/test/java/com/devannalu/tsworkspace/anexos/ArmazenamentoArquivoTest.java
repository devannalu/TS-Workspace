package com.devannalu.tsworkspace.anexos;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
class ArmazenamentoArquivoTest {
    @Test void validaAssinaturasERecusaExecutavelRenomeado() {
        assertThat(ArmazenamentoArquivo.conteudoValido("%PDF-1.7\n".getBytes(StandardCharsets.UTF_8),"application/pdf")).isTrue();
        assertThat(ArmazenamentoArquivo.conteudoValido("MZ executavel".getBytes(StandardCharsets.UTF_8),"application/pdf")).isFalse();
        assertThat(ArmazenamentoArquivo.conteudoValido("<svg onload='x'/>".getBytes(StandardCharsets.UTF_8),"text/plain")).isFalse();
        assertThat(ArmazenamentoArquivo.conteudoValido("#!/bin/sh".getBytes(StandardCharsets.UTF_8),"text/plain")).isFalse();
        assertThat(ArmazenamentoArquivo.conteudoValido(new byte[]{0,1,2},"text/csv")).isFalse();
        assertThat(ArmazenamentoArquivo.conteudoValido(new byte[]{(byte)255},"text/plain")).isFalse();
        assertThat(ArmazenamentoArquivo.conteudoValido("nome,valor\nAna,3".getBytes(StandardCharsets.UTF_8),"text/csv")).isTrue();
    }
}
