package com.devannalu.tsworkspace.emails;

import com.devannalu.tsworkspace.convites.ConviteService.ConviteCriadoResponse;
import com.devannalu.tsworkspace.usuarios.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.InternetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

@Service
public class ServicoEmail {
    private static final Logger LOG=LoggerFactory.getLogger(ServicoEmail.class);
    public enum Situacao { ENVIADO, DESATIVADO, FALHOU }
    private final JavaMailSender correio;
    private final UsuarioRepository usuarias;
    private final boolean habilitado;
    private final String remetente;
    private final String origem;

    public ServicoEmail(JavaMailSender correio,UsuarioRepository usuarias,@Value("${app.email.enabled:false}") boolean habilitado,
        @Value("${app.email.from}") String remetente,@Value("${app.frontend-origin}") String origem) {
        this.correio=correio;this.usuarias=usuarias;this.habilitado=habilitado;this.remetente=remetente;this.origem=origem.replaceAll("/$","");
    }
    @PostConstruct
    void validarConfiguracao() {
        if(!habilitado)return;
        try {
            new InternetAddress(remetente,true).validate();
            var uri=URI.create(origem);
            if(remetente.contains("\r")||remetente.contains("\n")||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null
                ||!("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw new IllegalArgumentException();
        }catch(Exception e){throw new IllegalStateException("Configure EMAIL_FROM e FRONTEND_ORIGIN válidos para o envio de e-mails.");}
    }
    private String escapar(String texto) {return HtmlUtils.htmlEscape(texto,StandardCharsets.UTF_8.name());}
    private Situacao enviar(String destinataria,String assunto,String texto,String corpoHtml) {
        if(!habilitado)return Situacao.DESATIVADO;
        // O SMTP nunca participa da transação que cria o acesso ou o convite.
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Envio de e-mail deve ocorrer após a transação.");
        try {
            var mensagem=correio.createMimeMessage();var helper=new MimeMessageHelper(mensagem,true,StandardCharsets.UTF_8.name());
            helper.setFrom(remetente);helper.setTo(new InternetAddress(destinataria,true));helper.setSubject(assunto);
            String html="<!doctype html><html lang=\"pt-BR\"><body style=\"margin:0;background:#faf7f2;color:#372b35;font-family:Arial,sans-serif\"><table role=\"presentation\" width=\"100%\"><tr><td style=\"padding:32px 16px\"><div style=\"max-width:560px;margin:auto;background:#fffdf9;border:1px solid #eee3e8;border-radius:16px;padding:28px\"><p style=\"color:#a33d70;font-weight:bold\">TECH SISTERS · TS WORKSPACE</p>"+corpoHtml+"<p style=\"margin-top:28px;color:#766a72;font-size:12px\">Um espaço nosso. Tech Sisters.</p></div></td></tr></table></body></html>";
            helper.setText(texto,html);correio.send(mensagem);return Situacao.ENVIADO;
        }catch(Exception e){
            // Exceções SMTP podem conter destinatária, senha ou link privado.
            LOG.warn("Não foi possível concluir um envio transacional de e-mail.");return Situacao.FALHOU;
        }
    }
    public Situacao enviarConvite(ConviteCriadoResponse convite) {
        String link=convite.inviteUrl(),prazo=java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm 'UTC'").withZone(java.time.ZoneOffset.UTC).format(convite.invite().expiresAt());
        String texto="Você recebeu um convite para a Tech Sisters.\n\nAcesse o TS Workspace: "+link+"\n\nO link é pessoal, de uso único e expira em "+prazo+".\nSe não esperava este convite, ignore esta mensagem.";
        return enviar(convite.invite().email(),"Seu convite para o TS Workspace",texto,
            "<h1 style=\"font-size:24px\">Seu espaço na Tech Sisters</h1><p>Você recebeu um convite para participar do TS Workspace.</p><p><a style=\"display:inline-block;padding:14px 20px;border-radius:12px;background:#a33d70;color:#ffffff;text-decoration:none\" href=\""+escapar(link)+"\">Aceitar convite</a></p><p>Link pessoal e de uso único. Expira em <strong>"+escapar(prazo)+"</strong>.</p><p>Se não esperava este convite, ignore esta mensagem.</p>");
    }
    public Situacao confirmarConta(String usuarioId) {
        if(!habilitado)return Situacao.DESATIVADO;
        var pessoa=usuarias.findById(usuarioId).orElse(null);
        if(pessoa==null)return Situacao.FALHOU;
        String link=origem+"/login";
        return enviar(pessoa.getEmail(),"Seu acesso ao TS Workspace está pronto",
            "Olá, "+pessoa.getNome()+". Seu convite foi aceito e seu acesso está pronto. Entre em "+link+" com o e-mail e a senha que você cadastrou.",
            "<h1 style=\"font-size:24px\">Bem-vinda ao Workspace</h1><p>Olá, "+escapar(pessoa.getNome())+". Seu acesso está pronto.</p><p><a style=\"color:#a33d70\" href=\""+escapar(link)+"\">Entrar no TS Workspace</a></p><p>Use o e-mail e a senha que você cadastrou. Nunca enviamos senhas por e-mail.</p>");
    }
}
