package com.devannalu.tsworkspace.convites;

import com.devannalu.tsworkspace.auth.EmailNormalizer;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.time.Instant;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PoliticaConviteTest {
    @Test void deveGerarTokensSegurosDistintosDe32Bytes() {
        var tokens=new HashSet<String>();
        for(int i=0;i<100;i++){String token=PoliticaConvite.gerarToken();assertThat(token).matches("[0-9a-f]{64}");tokens.add(token);}
        assertThat(tokens).hasSize(100);
    }
    @Test void deveCalcularHashSha256ConformeVetorConhecido() {
        assertThat(PoliticaConvite.calcularHashToken("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
    @Test void devePriorizarConviteUtilizadoSobreCancelamentoEExpiracao() {
        assertThat(PoliticaConvite.calcularSituacao(Instant.EPOCH,Instant.EPOCH,Instant.EPOCH,Instant.now())).isEqualTo(PoliticaConvite.SituacaoConvite.USED);
    }
    @Test void devePriorizarCancelamentoSobreExpiracao() {
        assertThat(PoliticaConvite.calcularSituacao(null,Instant.EPOCH,Instant.EPOCH,Instant.now())).isEqualTo(PoliticaConvite.SituacaoConvite.CANCELLED);
    }
    @Test void deveRespeitarLimiteDeExpiracao() {
        Instant now=Instant.now();
        assertThat(PoliticaConvite.calcularSituacao(null,null,now,now)).isEqualTo(PoliticaConvite.SituacaoConvite.EXPIRED);
        assertThat(PoliticaConvite.calcularSituacao(null,null,now.plusSeconds(1),now)).isEqualTo(PoliticaConvite.SituacaoConvite.PENDING);
        assertThat(PoliticaConvite.PRAZO_PADRAO_CONVITE_DIAS).isEqualTo(7);
    }
    @Test void deveRejeitarSituacoesTerminaisComMensagemPublicaUnica() {
        Instant now=Instant.now();
        assertThatThrownBy(()->PoliticaConvite.exigirConvitePendente(now,null,now.plusSeconds(1),now)).isInstanceOf(ProblemaDominio.class).hasMessage("Convite inválido ou indisponível.");
        assertThatThrownBy(()->PoliticaConvite.exigirConvitePendente(null,now,now.plusSeconds(1),now)).hasMessage("Convite inválido ou indisponível.");
        assertThatThrownBy(()->PoliticaConvite.exigirConvitePendente(null,null,now,now)).hasMessage("Convite inválido ou indisponível.");
    }
    @Test void deveRejeitarFormatoInvalidoDoToken() {
        assertThat(PoliticaConvite.possuiFormatoValido(null)).isFalse();assertThat(PoliticaConvite.possuiFormatoValido("invalid")).isFalse();
        assertThat(PoliticaConvite.possuiFormatoValido("a".repeat(64))).isTrue();
    }
    @Test void deveNormalizarEmailSemDependerDaLocalidade() {
        assertThat(EmailNormalizer.normalize(" MEMBER@Example.TEST ")).isEqualTo("member@example.test");
        assertThatThrownBy(()->EmailNormalizer.normalize(" ")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deveValidarNomeSenhaEConfirmacaoNoAceite() {
        String password="Testing password 2026";
        assertThatCode(()->PoliticaConvite.validarAceiteConvite(" Test ",password,password)).doesNotThrowAnyException();
        assertThatThrownBy(()->PoliticaConvite.validarAceiteConvite(" a ",password,password)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->PoliticaConvite.validarAceiteConvite("Test","short","short")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->PoliticaConvite.validarAceiteConvite("Test",password,"different")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->PoliticaConvite.validarAceiteConvite("Test","é".repeat(40),"é".repeat(40))).isInstanceOf(IllegalArgumentException.class);
    }
}
