package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.autenticacao.InicializacaoIdentidadeService;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.rbac.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class ChatIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_chat_test").withLabel("com.tsworkspace.purpose", "chat-test");
    @DynamicPropertySource static void banco(DynamicPropertyRegistry configuracao) {
        configuracao.add("spring.datasource.url", MYSQL::getJdbcUrl);
        configuracao.add("spring.datasource.username", MYSQL::getUsername);
        configuracao.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired InicializacaoIdentidadeService identidade;
    @Autowired RbacSeed seed;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired MockMvc http;
    @Autowired Flyway flyway;
    @Autowired com.devannalu.tsworkspace.chat.ChatService chat;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM chat_message"); jdbc.update("DELETE FROM workspace_note"); jdbc.update("DELETE FROM partnership"); jdbc.update("DELETE FROM communication_content"); jdbc.update("DELETE FROM community_event"); jdbc.update("DELETE FROM meeting"); jdbc.update("DELETE FROM attachment"); jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM workspace_comment"); jdbc.update("DELETE FROM task"); jdbc.update("DELETE FROM project");
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
    @Test void canaisDerivamDeEquipesEGeralExigePermissao() {
        assertThat(chat.canais(suporte)).extracting(c->c.id()).containsExactly("geral",equipeA);
        assertThat(chat.canais(superAdmin)).extracting(c->c.id()).contains(equipeA,equipeB);
        chat.enviar(suporte,"geral","Conversa geral");assertThat(chat.listar(fora,"geral",null,50).items()).hasSize(1);
        negar(suporte,"chat.view");assertThatThrownBy(()->chat.canais(suporte)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void cursorPreservaOrdemEHistoricoComMensagensNovas() {
        for(int i=0;i<4;i++)chat.enviar(suporte,equipeA,"Mensagem "+i);
        jdbc.update("UPDATE chat_message SET created_at='2026-10-13 12:00:00'");
        var primeira=chat.listar(suporte,equipeA,null,2);assertThat(primeira.items()).hasSize(2);assertThat(primeira.proximaPagina()).isNotNull();
        chat.enviar(suporte,equipeA,"Nova mensagem");
        var anterior=chat.listar(suporte,equipeA,primeira.proximaPagina(),2);assertThat(anterior.items()).hasSize(2);assertThat(anterior.proximaPagina()).isNull();
        var ids=new HashSet<String>();primeira.items().forEach(m->ids.add(m.id()));anterior.items().forEach(m->ids.add(m.id()));assertThat(ids).hasSize(4);
        var outro=chat.enviar(fora,equipeB,"Outro canal");assertThatThrownBy(()->chat.listar(suporte,equipeA,outro.id(),2)).isInstanceOf(ProblemaDominio.class);
    }
    @Test void propriaEditaComVersaoERemocaoNaoExpoeTexto() {
        var mensagem=chat.enviar(suporte,equipeA,"Texto privado");
        assertThatThrownBy(()->chat.editar(outra,mensagem.id(),"Outro",0)).isInstanceOf(AccessDeniedException.class);
        var editada=chat.editar(suporte,mensagem.id(),"Revisão",0);assertThat(editada.versao()).isEqualTo(1);
        assertThatThrownBy(()->chat.editar(suporte,mensagem.id(),"Outra",0)).isInstanceOf(ProblemaDominio.class);
        var removida=chat.remover(suporte,mensagem.id(),1);assertThat(removida.texto()).isNull();assertThat(removida.removida()).isTrue();
        var leitura=chat.listar(outra,equipeA,null,50).items().get(0);assertThat(leitura.texto()).isNull();assertThat(leitura.capacidades().editar()).isFalse();
        assertThatThrownBy(()->chat.remover(suporte,mensagem.id(),2)).isInstanceOf(ProblemaDominio.class);
    }
    @Test void moderacaoExigePermissaoEContinuaNoEscopo() {
        var mensagem=chat.enviar(suporte,equipeA,"Conversa");
        assertThatThrownBy(()->chat.remover(supervisora,mensagem.id(),0)).isInstanceOf(AccessDeniedException.class);
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ? ,id,'ALLOW' FROM permissions WHERE permission_key='chat.moderate'",outra);
        assertThat(chat.remover(outra,mensagem.id(),0).removida()).isTrue();
        var externa=chat.enviar(fora,equipeB,"Outra equipe");assertThatThrownBy(()->chat.remover(outra,externa.id(),0)).isInstanceOf(AccessDeniedException.class);
        assertThat(chat.remover(admin,externa.id(),0).removida()).isTrue();
    }
    @Test void membershipRevogadaImpedeLeituraEEscritaPropria() {
        var mensagem=chat.enviar(suporte,equipeA,"Conversa");
        jdbc.update("DELETE FROM team_member WHERE user_id=? AND team_id=?",suporte,equipeA);
        assertThat(chat.canais(suporte)).extracting(c->c.id()).doesNotContain(equipeA);
        assertThatThrownBy(()->chat.listar(suporte,equipeA,null,50)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.editar(suporte,mensagem.id(),"Outra",0)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void equipeArquivadaMantemHistoricoSomenteLeitura() {
        var mensagem=chat.enviar(suporte,equipeA,"Histórico");jdbc.update("UPDATE team SET archived_at=NOW(6) WHERE id=?",equipeA);
        assertThat(chat.canais(suporte).stream().filter(c->c.id().equals(equipeA)).findFirst().orElseThrow().arquivado()).isTrue();
        assertThat(chat.listar(suporte,equipeA,null,50).podeEnviar()).isFalse();
        assertThatThrownBy(()->chat.enviar(suporte,equipeA,"Outra")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.remover(admin,mensagem.id(),0)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void revogacaoDeEnvioValeNaConversaAtual() {
        var mensagem=chat.enviar(suporte,"geral","Conversa");negar(suporte,"chat.send");
        var leitura=chat.listar(suporte,"geral",null,50);assertThat(leitura.podeEnviar()).isFalse();assertThat(leitura.items().get(0).capacidades().editar()).isFalse();
        assertThatThrownBy(()->chat.enviar(suporte,"geral","Outra")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->chat.remover(suporte,mensagem.id(),0)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void textoLimitesSeedEAuditoriaSemConteudo() {
        assertThatThrownBy(()->chat.enviar(suporte,"geral"," ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->chat.enviar(suporte,"geral","x".repeat(2001))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->chat.listar(suporte,"geral",null,101)).isInstanceOf(IllegalArgumentException.class);
        var mensagem=chat.enviar(suporte,"geral","<script>segredo operacional</script>");assertThat(mensagem.texto()).startsWith("<script>");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE entity_type='ChatMessage'",Integer.class)).isZero();
        chat.editar(suporte,mensagem.id(),"Revisão",0);chat.remover(admin,mensagem.id(),1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE entity_type='ChatMessage'",Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE metadata_json LIKE '%segredo operacional%'",Integer.class)).isZero();
        seed.seed();seed.seed();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permissions WHERE permission_key LIKE 'chat.%'",Integer.class)).isEqualTo(3);
    }
    @Test void http401403404409ECsrf() throws Exception {
        String rota="/api/v1/chat/channels/geral/messages";var corpo=Map.of("texto","Mensagem");
        http.perform(get(rota)).andExpect(status().isUnauthorized());var sessao=entrar(suporte);
        http.perform(post(rota).cookie(sessao).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isForbidden());
        var resposta=http.perform(post(rota).cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isCreated()).andReturn().getResponse();
        String id=json.readTree(resposta.getContentAsString()).get("id").asText();
        http.perform(patch("/api/v1/chat/messages/"+id).cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("texto","Revisão","versao",99)))).andExpect(status().isConflict());
        http.perform(patch("/api/v1/chat/messages/"+UUID.randomUUID()).cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("texto","Revisão","versao",0)))).andExpect(status().isNotFound());
        http.perform(get("/api/v1/chat/channels/"+equipeB+"/messages").cookie(sessao)).andExpect(status().isForbidden());
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?",suporte);
        http.perform(get(rota).cookie(sessao)).andExpect(status().isForbidden());
        http.perform(get(rota).cookie(sessao)).andExpect(status().isUnauthorized());
    }
    @Test void timestampsPreservamInstantEmFusosDiferentes() {
        var original=TimeZone.getDefault();
        try {
            for(String zona:List.of("America/Bahia","Europe/Lisbon","Pacific/Kiritimati")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zona));
                var inicio=Instant.now();var mensagem=chat.enviar(suporte,"geral","Conversa no fuso "+zona);
                assertThat(mensagem.criadaEm()).isBetween(inicio.minusSeconds(5),Instant.now().plusSeconds(5));
                double banco=jdbc.queryForObject("SELECT UNIX_TIMESTAMP(created_at) FROM chat_message WHERE id=?",Double.class,mensagem.id());
                assertThat(Math.abs(banco-mensagem.criadaEm().getEpochSecond())).isLessThan(1);
                var atualizada=chat.editar(suporte,mensagem.id(),"Conversa revisada",0);
                assertThat(atualizada.atualizadaEm()).isBetween(inicio.minusSeconds(5),Instant.now().plusSeconds(5));
                LocalDate data=jdbc.queryForObject("SELECT CAST('2026-10-13' AS DATE)",(r,i)->r.getObject(1,LocalDate.class));
                assertThat(data).isEqualTo(LocalDate.of(2026,10,13));
            }
        } finally {TimeZone.setDefault(original);}
    }
    @Test void concorrenciaMantemUmaEdicaoVencedora() throws Exception {
        var mensagem=chat.enviar(suporte,"geral","Conversa");var inicio=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {var resultados=new ArrayList<java.util.concurrent.Future<Boolean>>();for(int i=0;i<2;i++)resultados.add(pool.submit(()->{inicio.await();try{chat.editar(suporte,mensagem.id(),"Revisão",0);return true;}catch(ProblemaDominio e){assertThat(e.status()).isEqualTo(409);return false;}}));
            inicio.countDown();int sucesso=0;for(var resultado:resultados)if(resultado.get(15,java.util.concurrent.TimeUnit.SECONDS))sucesso++;assertThat(sucesso).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
}
