package com.devannalu.tsworkspace.emails;

import com.devannalu.tsworkspace.convites.ConviteService.*;
import com.devannalu.tsworkspace.convites.PoliticaConvite;
import com.devannalu.tsworkspace.usuarios.*;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.*;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class ServicoEmailTest {
    JavaMailSender correio;
    UsuarioRepository usuarias;
    ServicoEmail servico;
    @BeforeEach void preparar() {
        correio=mock(JavaMailSender.class);usuarias=mock(UsuarioRepository.class);
        when(correio.createMimeMessage()).thenAnswer(i->new MimeMessage(Session.getInstance(new Properties())));
        servico=new ServicoEmail(correio,usuarias,true,"workspace@example.test","https://workspace.example.test");
    }
    private ConviteCriadoResponse convite() {
        String token="token-apenas-para-template";
        var convite=new ConviteResponse(UUID.randomUUID().toString(),"integrante@example.test",null,List.of(),null,Instant.now(),Instant.parse("2026-10-24T18:00:00Z"),PoliticaConvite.SituacaoConvite.PENDING);
        return new ConviteCriadoResponse(convite,token,"https://workspace.example.test/convite/"+token);
    }
    private String conteudo(Part parte) throws Exception {
        Object valor=parte.getContent();
        if(valor instanceof String texto)return texto;
        if(valor instanceof Multipart partes) {var texto=new StringBuilder();for(int i=0;i<partes.getCount();i++)texto.append(conteudo(partes.getBodyPart(i)));return texto.toString();}
        return "";
    }
    @Test void desativadoNaoContataSmtpNemConsultaConta() {
        servico=new ServicoEmail(correio,usuarias,false,"workspace@example.test","https://workspace.example.test");
        assertThat(servico.enviarConvite(convite())).isEqualTo(ServicoEmail.Situacao.DESATIVADO);
        assertThat(servico.confirmarConta("id")).isEqualTo(ServicoEmail.Situacao.DESATIVADO);verifyNoInteractions(correio,usuarias);
    }
    @Test void convitePossuiTextoHtmlLinkUnicoEPrazo() throws Exception {
        var convite=convite();assertThat(servico.enviarConvite(convite)).isEqualTo(ServicoEmail.Situacao.ENVIADO);
        var capturada=ArgumentCaptor.forClass(MimeMessage.class);verify(correio).send(capturada.capture());
        var mensagem=capturada.getValue();mensagem.saveChanges();
        assertThat(mensagem.getSubject()).isEqualTo("Seu convite para o TS Workspace");
        assertThat(mensagem.getAllRecipients()).hasSize(1);assertThat(mensagem.getAllRecipients()[0].toString()).isEqualTo("integrante@example.test");
        assertThat(conteudo(mensagem)).contains(convite.inviteUrl(),"uso único","TECH SISTERS","24/10/2026 às 18:00 UTC");
    }
    @Test void boasVindasEscapaNomeENaoEnviaSenha() throws Exception {
        var pessoa=new Usuario("<img src=x onerror=alert(1)>","integrante@example.test","hash-nao-pode-sair");
        when(usuarias.findById("id")).thenReturn(Optional.of(pessoa));
        assertThat(servico.confirmarConta("id")).isEqualTo(ServicoEmail.Situacao.ENVIADO);
        var capturada=ArgumentCaptor.forClass(MimeMessage.class);verify(correio).send(capturada.capture());
        String corpo=conteudo(capturada.getValue());assertThat(corpo).contains("&lt;img","https://workspace.example.test/login").doesNotContain("hash-nao-pode-sair");
    }
    @Test void falhaNaoPropagaDetalhesSensiveis(CapturedOutput saida) {
        doThrow(new MailSendException("password=segredo-email; integrante@example.test; token-privado")).when(correio).send(any(MimeMessage.class));
        assertThat(servico.enviarConvite(convite())).isEqualTo(ServicoEmail.Situacao.FALHOU);
        assertThat(saida.getAll()).doesNotContain("segredo-email","integrante@example.test","token-privado");
    }
    @Test void smtpNaoPodeSerExecutadoDentroDaTransacao() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {assertThatThrownBy(()->servico.enviarConvite(convite())).isInstanceOf(IllegalStateException.class);verifyNoInteractions(correio);}
        finally {TransactionSynchronizationManager.clear();}
    }
    @Test void configuracaoInvalidaFalhaSemRevelarValores() {
        servico=new ServicoEmail(correio,usuarias,true,"remetente-invalido","javascript:segredo-configuracao");
        assertThatThrownBy(servico::validarConfiguracao).isInstanceOf(IllegalStateException.class).hasMessageNotContaining("segredo-configuracao");
    }
    @Test void contaInexistenteNaoEnvia() {
        when(usuarias.findById("inexistente")).thenReturn(Optional.empty());
        assertThat(servico.confirmarConta("inexistente")).isEqualTo(ServicoEmail.Situacao.FALHOU);verifyNoInteractions(correio);
    }
}
