package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.autenticacao.InicializacaoIdentidadeService;
import com.devannalu.tsworkspace.rbac.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(properties={"app.email.enabled=true","app.frontend-origin=http://localhost:3010"}) @AutoConfigureMockMvc @Testcontainers
class EmailIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_email_test").withLabel("com.tsworkspace.purpose", "email-test");
    @Container static final org.testcontainers.containers.GenericContainer<?> MAILPIT = new org.testcontainers.containers.GenericContainer<>("axllent/mailpit:v1.31.6")
        .withExposedPorts(1025,8025).withLabel("com.tsworkspace.purpose","email-test");
    @DynamicPropertySource static void banco(DynamicPropertyRegistry configuracao) {
        configuracao.add("spring.mail.host", MAILPIT::getHost);
        configuracao.add("spring.mail.port", ()->MAILPIT.getMappedPort(1025));
        configuracao.add("spring.datasource.url", MYSQL::getJdbcUrl);
        configuracao.add("spring.datasource.username", MYSQL::getUsername);
        configuracao.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired InicializacaoIdentidadeService identidade;
    @Autowired RbacSeed seed;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired MockMvc http;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean org.springframework.mail.javamail.JavaMailSender correio;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM invite_team");jdbc.update("DELETE FROM invite");
        jdbc.update("DELETE FROM workspace_note"); jdbc.update("DELETE FROM partnership"); jdbc.update("DELETE FROM communication_content"); jdbc.update("DELETE FROM community_event"); jdbc.update("DELETE FROM meeting"); jdbc.update("DELETE FROM attachment"); jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM workspace_comment"); jdbc.update("DELETE FROM task"); jdbc.update("DELETE FROM project");
        jdbc.update("DELETE FROM team_member"); jdbc.update("DELETE FROM user_permissions");
        jdbc.update("DELETE FROM app_profile"); jdbc.update("DELETE FROM app_user");
        jdbc.update("UPDATE team SET archived_at=NULL");
        seed.seed(); identidade.criarPrimeiraIdentidade("Teste Super Admin", "admin@example.test", SENHA_TESTE);
        superAdmin = jdbc.queryForObject("SELECT id FROM app_user WHERE email='admin@example.test'", String.class);
        String hash = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=?", String.class, superAdmin);
        admin = criarUsuario("ADMIN", hash); supervisora = criarUsuario("SUPERVISOR", hash);
        suporte = criarUsuario("SUPPORT", hash); outra = criarUsuario("SUPPORT", hash); fora = criarUsuario("SUPPORT", hash);
        equipeA = jdbc.queryForObject("SELECT id FROM team WHERE team_key='comunicacao'", String.class);
        equipeB = jdbc.queryForObject("SELECT id FROM team WHERE team_key='eventos'", String.class);
        for (String pessoa : List.of(supervisora, suporte, outra)) vincular(pessoa, equipeA);
        vincular(fora, equipeB);
    }

    private String criarUsuario(String perfil, String hash) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO app_user VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))", id, "Teste " + perfil, id + "@example.test", hash);
        jdbc.update("INSERT INTO app_profile (user_id,role_id,status,created_at,updated_at) SELECT ?,id,'ACTIVE',CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6) FROM roles WHERE role_key=?", id, perfil);
        return id;
    }
    private void vincular(String pessoa, String equipe) {
        jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))", pessoa, equipe);
    }
    private void negar(String pessoa, String permissao) {
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key=?", pessoa, permissao);
    }
    private Cookie entrar(String usuario) throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM app_user WHERE id=?", String.class, usuario);
        return http.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", SENHA_TESTE))))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("TS_SESSION");
    }


    private com.fasterxml.jackson.databind.JsonNode correio(String caminho) throws Exception {
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://"+MAILPIT.getHost()+":"+MAILPIT.getMappedPort(8025)+"/api/v1"+caminho)).GET().build();
        var resposta=java.net.http.HttpClient.newHttpClient().send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(resposta.statusCode()).isEqualTo(200);return json.readTree(resposta.body());
    }
    private com.fasterxml.jackson.databind.JsonNode criar(String email) throws Exception {
        String perfil=jdbc.queryForObject("SELECT id FROM roles WHERE role_key='SUPPORT'",String.class);
        var resposta=http.perform(post("/api/v1/invites").cookie(entrar(superAdmin)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",email,"roleId",perfil,"teamIds",List.of(equipeA))))).andExpect(status().isCreated()).andReturn();
        return json.readTree(resposta.getResponse().getContentAsString());
    }
    @Test void smtpRealRecebeConviteComTokenUnicoAposCommit() throws Exception {
        String email=UUID.randomUUID()+"@example.test";var resposta=criar(email);
        assertThat(resposta.path("envioEmail").asText()).isEqualTo("ENVIADO");
        var lista=correio("/messages").path("messages");String id=null;
        for(var mensagem:lista)if(mensagem.path("To").get(0).path("Address").asText().equals(email))id=mensagem.path("ID").asText();
        assertThat(id).isNotNull();var mensagem=correio("/message/"+id);
        assertThat(mensagem.path("Text").asText()).contains(resposta.path("inviteUrl").asText(),"uso único");
        assertThat(mensagem.path("HTML").asText()).contains("TECH SISTERS","Aceitar convite");
        String hash=jdbc.queryForObject("SELECT token_hash FROM invite WHERE id=?",String.class,resposta.path("invite").path("id").asText());
        assertThat(hash).isEqualTo(com.devannalu.tsworkspace.convites.PoliticaConvite.calcularHashToken(resposta.path("token").asText()));
        String listagem=http.perform(get("/api/v1/invites").cookie(entrar(superAdmin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(listagem).doesNotContain(resposta.path("token").asText(),hash,"inviteUrl");
    }
    @Test void falhaSmtpPreservaConviteEOfereceLinkManual() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("Falha SMTP simulada"))
            .when(correio).send(org.mockito.ArgumentMatchers.any(jakarta.mail.internet.MimeMessage.class));
        var resposta=criar(UUID.randomUUID()+"@example.test");
        assertThat(resposta.path("envioEmail").asText()).isEqualTo("FALHOU");assertThat(resposta.path("inviteUrl").asText()).contains("/convite/");
        http.perform(post("/api/v1/invites/validate").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("token",resposta.path("token").asText())))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM invite WHERE id=?",Integer.class,resposta.path("invite").path("id").asText())).isEqualTo(1);
    }
    @Test void aceiteEnviaBoasVindasSemDuplicarContaOuReutilizarToken() throws Exception {
        String email=UUID.randomUUID()+"@example.test";var convite=criar(email);
        var dados=Map.of("token",convite.path("token").asText(),"name","<img src=x>","password",SENHA_TESTE,"passwordConfirmation",SENHA_TESTE);
        http.perform(post("/api/v1/invites/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados))).andExpect(status().isCreated());
        boolean recebeu=false;
        for(var mensagem:correio("/messages").path("messages"))if(mensagem.path("To").get(0).path("Address").asText().equals(email)&&mensagem.path("Subject").asText().contains("está pronto")) {
            recebeu=true;var corpo=correio("/message/"+mensagem.path("ID").asText());
            assertThat(corpo.path("HTML").asText()).contains("&lt;img","/login").doesNotContain("<img src=x>",SENHA_TESTE);
            assertThat(corpo.path("Text").asText()).doesNotContain(SENHA_TESTE,convite.path("token").asText());
        }
        assertThat(recebeu).isTrue();
        http.perform(post("/api/v1/invites/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados))).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email=?",Integer.class,email)).isEqualTo(1);
    }
    @Test void naoEnviaParaCriacaoNaoAutorizadaOuInvalida() throws Exception {
        int antes=correio("/messages").path("total").asInt();
        http.perform(post("/api/v1/invites").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        String perfil=jdbc.queryForObject("SELECT id FROM roles WHERE role_key='SUPPORT'",String.class);
        http.perform(post("/api/v1/invites").cookie(entrar(suporte)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email","sem-permissao@example.test","roleId",perfil,"teamIds",List.of(equipeA))))).andExpect(status().isForbidden());
        http.perform(post("/api/v1/invites").cookie(entrar(superAdmin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        assertThat(correio("/messages").path("total").asInt()).isEqualTo(antes);
    }
}
