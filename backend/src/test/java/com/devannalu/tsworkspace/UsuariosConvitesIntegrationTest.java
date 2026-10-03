package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.auth.*;
import com.devannalu.tsworkspace.usuarios.*;
import com.devannalu.tsworkspace.autenticacao.*;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.convites.*;
import com.devannalu.tsworkspace.equipes.InicializacaoEquipesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class UsuariosConvitesIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.4.11").withDatabaseName("ts_workspace_users_invites_test").withLabel("com.tsworkspace.purpose","users-invites-test");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);}
    @Autowired JdbcTemplate jdbc;
    @Autowired ConviteService invites;
    @Autowired UsuarioService management;
    @Autowired InicializacaoIdentidadeService bootstrap;
    @Autowired UsuarioRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired InicializacaoEquipesService seed;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired Flyway flyway;
    String admin,root,role,communication;
    static final String PASSWORD="Users invites test password";
    @BeforeEach void reset(){
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES");jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM audit_log");jdbc.update("DELETE FROM invite_team");jdbc.update("DELETE FROM invite");
        jdbc.update("DELETE FROM team_member");jdbc.update("DELETE FROM user_permissions");jdbc.update("DELETE FROM app_profile");jdbc.update("DELETE FROM app_user");
        jdbc.update("UPDATE team SET archived_at=NULL");
        admin=identity("SUPER_ADMIN");seed.seed();
        root=jdbc.queryForObject("SELECT id FROM team WHERE team_key='fundadoras'",String.class);
        communication=jdbc.queryForObject("SELECT id FROM team WHERE team_key='comunicacao'",String.class);
        role=role("SUPPORT");
    }
    @Test void devePreservarMigrationsERegrasDoCheckpoint() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("7");assertThat(flyway.migrate().migrationsExecuted).isZero();
        try(var input=getClass().getResourceAsStream("/users-invites-baseline.json")){
            var baseline=mapper.readTree(input);assertThat(PoliticaConvite.PRAZO_PADRAO_CONVITE_DIAS).isEqualTo(baseline.path("ttlDays").asInt());
            assertThat(java.util.HexFormat.of().parseHex(PoliticaConvite.gerarToken())).hasSize(baseline.path("entropyBytes").asInt());
            Instant now=Instant.now();
            for(var c:baseline.path("states"))assertThat(PoliticaConvite.calcularSituacao(c.path("used").asBoolean()?now:null,c.path("cancelled").asBoolean()?now:null,c.path("future").asBoolean()?now.plusSeconds(1):now,now).name()).isEqualTo(c.path("state").asText());
            for(var c:baseline.path("adminCases")){
                boolean allowed;
                try{PoliticaUsuario.protegerAcessoAdministrativo(c.path("self").asBoolean()?"target":"actor","target",c.path("role").asText(),true,c.path("nextRole").asText(),c.path("nextActive").asBoolean(),c.path("count").asLong());allowed=true;}catch(ProblemaDominio e){allowed=false;}
                assertThat(allowed).isEqualTo(c.path("allowed").asBoolean());
            }
        }
    }
    @Test void deveContarSomenteConvitesPendentesComPermissao() throws Exception {
        var pending = invite("pending-count@example.test");
        var cancelled = invite("cancelled-count@example.test"); invites.cancelarConvite(admin, cancelled.invite().id());
        var expired = invite("expired-count@example.test");
        jdbc.update("UPDATE invite SET expires_at=DATE_SUB(CURRENT_TIMESTAMP(6),INTERVAL 1 DAY) WHERE id=?", expired.invite().id());
        String support = accept("used-count@example.test").userId();
        assertThat(invites.contarConvitesPendentes()).isEqualTo(1);
        mvc.perform(get("/api/v1/invites/pending-count").cookie(login(admin))).andExpect(status().isOk()).andExpect(content().string("1"));
        mvc.perform(get("/api/v1/invites/pending-count").cookie(login(support))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/invites/pending-count")).andExpect(status().isUnauthorized());
        invites.cancelarConvite(admin, pending.invite().id()); assertThat(invites.contarConvitesPendentes()).isZero();
    }
    @Test void deveCriarConviteComHashPrazoPadraoERespostaSegura() throws Exception {
        Instant before=Instant.now();var created=invite(" member@example.test ");
        assertThat(created.invite().email()).isEqualTo("member@example.test");
        assertThat(created.invite().expiresAt()).isBetween(before.plus(Duration.ofDays(7)),Instant.now().plus(Duration.ofDays(7)));
        assertThat(jdbc.queryForObject("SELECT token_hash FROM invite WHERE id=?",String.class,created.invite().id())).isEqualTo(PoliticaConvite.calcularHashToken(created.token())).isNotEqualTo(created.token());
        assertThat(created.invite().teams()).hasSize(2);assertThat(created.invite().role().id()).isEqualTo(role);
        String json=mapper.writeValueAsString(invites.listarConvites(0,10));assertThat(json).doesNotContain(created.token(),"tokenHash","token_hash","inviteUrl");
        assertThat(invites.listarConvites(0,1).total()).isEqualTo(1);assertThat(invites.listarConvites(1,1).items()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='invite.created'",Long.class)).isEqualTo(1);
    }
    @Test void deveAceitarConviteAtomicamenteSemCriarSessao() {
        var invitation=invite("accepted@example.test");var accepted=invites.aceitarConvite(invitation.token()," Invited Test ",PASSWORD,PASSWORD);
        var user=management.buscarUsuario(accepted.userId());assertThat(user.email()).isEqualTo(invitation.invite().email());assertThat(user.name()).isEqualTo("Invited Test");
        assertThat(user.status()).isEqualTo("ACTIVE");assertThat(user.role().id()).isEqualTo(role);assertThat(user.teams()).extracting(UsuarioService.EquipeReferencia::id).containsExactlyInAnyOrder(root,communication);
        String hash=jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=?",String.class,user.id());assertThat(passwords.matches(PASSWORD,hash)).isTrue();assertThat(hash).startsWith("$2a$12$");
        assertThat(jdbc.queryForObject("SELECT used_at FROM invite WHERE id=?",java.sql.Timestamp.class,accepted.inviteId())).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='invite.accepted' AND actor_id=?",Long.class,user.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION",Long.class)).isZero();
        assertThatThrownBy(()->invites.aceitarConvite(invitation.token(),"Repeat",PASSWORD,PASSWORD)).hasMessage("Convite inválido ou indisponível.");
        assertThatThrownBy(()->invites.cancelarConvite(admin,accepted.inviteId())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void deveRejeitarConvitesIndisponiveisComMensagemGenerica() {
        var cancelled=invite("cancelled@example.test");invites.cancelarConvite(admin,cancelled.invite().id());
        assertThatThrownBy(()->invites.cancelarConvite(admin,cancelled.invite().id())).isInstanceOf(ProblemaDominio.class);
        var expired=invite("expired@example.test");jdbc.update("UPDATE invite SET expires_at=CURRENT_TIMESTAMP(6) WHERE id=?",expired.invite().id());
        var archived=invite("archived@example.test");jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP(6) WHERE id=?",communication);
        for(String token:List.of("invalid",PoliticaConvite.gerarToken(),cancelled.token(),expired.token(),archived.token())){
            assertThatThrownBy(()->invites.consultarConvitePublico(token)).hasMessage("Convite inválido ou indisponível.");
            assertThatThrownBy(()->invites.aceitarConvite(token,"Test",PASSWORD,PASSWORD)).hasMessage("Convite inválido ou indisponível.");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='invite.cancelled'",Long.class)).isEqualTo(1);
    }
    @Test void deveValidarDuplicidadeEmailPerfilEEquipesDoConvite() {
        invite("pending@example.test");assertThatThrownBy(()->invite("PENDING@example.test")).hasMessageContaining("pendente");
        assertThatThrownBy(()->invite(users.findById(admin).orElseThrow().getEmail())).hasMessageContaining("Já existe uma usuária");
        assertThatThrownBy(()->invites.criarConvite(admin,"badrole@example.test",UUID.randomUUID().toString(),List.of(root))).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->invites.criarConvite(admin,"badteam@example.test",role,List.of(UUID.randomUUID().toString()))).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->invites.criarConvite(admin,"empty@example.test",role,List.of())).isInstanceOf(IllegalArgumentException.class);
        var expired=invite("repeat-expired@example.test");jdbc.update("UPDATE invite SET expires_at=CURRENT_TIMESTAMP(6) WHERE id=?",expired.invite().id());
        assertThatCode(()->invite("repeat-expired@example.test")).doesNotThrowAnyException();
    }
    @Test void deveGarantirCriacaoEAceiteUnicosSobConcorrencia() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try{
            var creation=pool.invokeAll(List.of(attempt(()->invite("concurrent@example.test")),attempt(()->invite("concurrent@example.test"))));
            assertThat(List.of(creation.get(0).get(),creation.get(1).get())).containsExactlyInAnyOrder(true,false);
            var invitation=invite("accept-race@example.test");
            var acceptance=pool.invokeAll(List.of(attempt(()->invites.aceitarConvite(invitation.token(),"A member",PASSWORD,PASSWORD)),attempt(()->invites.aceitarConvite(invitation.token(),"B member",PASSWORD,PASSWORD))));
            assertThat(List.of(acceptance.get(0).get(),acceptance.get(1).get())).containsExactlyInAnyOrder(true,false);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email='accept-race@example.test'",Long.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='invite.accepted'",Long.class)).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void deveReverterAceiteCompletoQuandoHouverFalha() {
        var invitation=invite("rollback@example.test");
        jdbc.execute("ALTER TABLE audit_log ADD CONSTRAINT test_reject_acceptance CHECK (action <> 'invite.accepted')");
        try{assertThatThrownBy(()->invites.aceitarConvite(invitation.token(),"Rollback",PASSWORD,PASSWORD)).isInstanceOf(org.springframework.dao.DataAccessException.class);}
        finally{jdbc.execute("ALTER TABLE audit_log DROP CHECK test_reject_acceptance");}
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email='rollback@example.test'",Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT used_at FROM invite WHERE id=?",java.sql.Timestamp.class,invitation.invite().id())).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_profile",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM team_member",Long.class)).isEqualTo(1);
        assertThatCode(()->invites.aceitarConvite(invitation.token(),"Retry",PASSWORD,PASSWORD)).doesNotThrowAnyException();
    }
    @Test void devePaginarFiltrarEEditarUsuariosComAuditoria() throws Exception {
        var accepted=accept("managed@example.test");String id=accepted.userId();
        var result=management.listarUsuarios(0,1,"ACTIVE",role,communication,"MANAGED");assertThat(result.total()).isEqualTo(1);assertThat(result.items()).hasSize(1);
        assertThat(management.listarUsuarios(1,1,null,null,null,null).items()).hasSize(1);
        assertThat(management.listarUsuarios(0,10,"INACTIVE",null,null,null).items()).isEmpty();
        var updated=management.editarUsuario(admin,id," Developer ",role("ADMIN"),List.of(communication,communication));
        assertThat(updated.jobTitle()).isEqualTo("Developer");assertThat(updated.role().key()).isEqualTo("ADMIN");assertThat(updated.teams()).hasSize(1);
        String json=mapper.writeValueAsString(updated);assertThat(json).doesNotContain("passwordHash","sessionId");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='user.role_changed'",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='user.teams_changed'",Long.class)).isEqualTo(1);
        management.editarUsuario(admin,id,"",null,null);assertThat(management.buscarUsuario(id).jobTitle()).isNull();
        assertThatThrownBy(()->management.listarUsuarios(0,101,null,null,null,null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->management.buscarUsuario(UUID.randomUUID().toString())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void deveRevogarSessoesNaInativacaoSemRestauraLasNaReativacao() throws Exception {
        String id=accept("sessions@example.test").userId();Cookie first=login(id),second=login(id);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME='sessions@example.test'",Long.class)).isEqualTo(2);
        management.alterarSituacaoUsuario(admin,id,false);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION WHERE PRINCIPAL_NAME='sessions@example.test'",Long.class)).isZero();
        mvc.perform(get("/api/v1/auth/me").cookie(first)).andExpect(status().isUnauthorized());
        loginRequest(id).andExpect(status().isUnauthorized());
        management.alterarSituacaoUsuario(admin,id,true);
        mvc.perform(get("/api/v1/auth/me").cookie(second)).andExpect(status().isUnauthorized());
        assertThat(login(id)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='user.status_changed'",Long.class)).isEqualTo(2);
    }
    @Test void deveProtegerUltimaAdminAcessoProprioEFundadoras() {
        assertThatThrownBy(()->management.alterarSituacaoUsuario("different",admin,false)).hasMessageContaining("última");
        assertThatThrownBy(()->management.editarUsuario("different",admin,null,role,List.of(root))).hasMessageContaining("última");
        assertThatThrownBy(()->management.editarUsuario("different",admin,null,null,List.of(communication))).hasMessageContaining("Fundadoras");
        String other=identity("SUPER_ADMIN");
        assertThatThrownBy(()->management.alterarSituacaoUsuario(admin,admin,false)).hasMessageContaining("próprio");
        assertThatThrownBy(()->management.editarUsuario(other,admin,null,role,null)).hasMessageContaining("Fundadoras");
        seed.seed();management.editarUsuario(other,admin,null,role,List.of(communication));
        assertThat(management.buscarUsuario(admin).role().key()).isEqualTo("SUPPORT");
    }
    @Test void devePreservarAdministracaoSobAlteracoesConcorrentes() throws Exception {
        String second=identity("SUPER_ADMIN"),actor=identity("SUPPORT");seed.seed();var pool=Executors.newFixedThreadPool(2);
        try{
            var results=pool.invokeAll(List.of(attempt(()->management.editarUsuario(actor,admin,null,role,null)),attempt(()->management.editarUsuario(actor,second,null,role,null))));
            assertThat(List.of(results.get(0).get(),results.get(1).get())).containsExactlyInAnyOrder(true,false);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_profile p JOIN roles r ON r.id=p.role_id WHERE p.status='ACTIVE' AND r.role_key='SUPER_ADMIN'",Long.class)).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void devePreservarAutorizacaoCsrfEAusenciaDeCadastroPublico() throws Exception {
        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());mvc.perform(get("/api/v1/invites")).andExpect(status().isUnauthorized());
        Cookie adminSession=login(admin),supportSession=login(accept("support@example.test").userId());
        mvc.perform(get("/api/v1/users").cookie(adminSession)).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/users").cookie(supportSession)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/invites").cookie(supportSession)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/users").cookie(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/invites/accept").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/invites/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        var invitation=invite("public@example.test");
        mvc.perform(post("/api/v1/invites/validate").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("token",invitation.token())))).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("public@example.test")).andExpect(jsonPath("$.id").doesNotExist()).andExpect(jsonPath("$.roleId").doesNotExist());
        mvc.perform(post("/api/v1/invites/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("token",invitation.token(),"name","Public member","password",PASSWORD,"passwordConfirmation",PASSWORD,"email","attacker@example.test","roleId",role("SUPER_ADMIN"),"teamIds",List.of())))).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email='attacker@example.test'",Long.class)).isZero();
        assertThat(management.listarUsuarios(0,25,null,role,null,"public@example.test").items()).hasSize(1);
    }
    @Test void deveExigirPermissoesEValidarAlteracoesAdministrativas() throws Exception {
        String target=accept("target@example.test").userId();Cookie support=login(target),session=login(admin);
        mvc.perform(patch("/api/v1/users/"+target).cookie(support).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"jobTitle\":\"Denied\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/users/"+target+"/deactivate").cookie(support).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/users/"+target+"/activate").cookie(support).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/invites").cookie(support).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email","denied@example.test","roleId",role,"teamIds",List.of(root))))).andExpect(status().isForbidden());
        var invitation=invite("cancel-denied@example.test");
        mvc.perform(post("/api/v1/invites/"+invitation.invite().id()+"/cancel").cookie(support).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/users/"+target).cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"jobTitle\":\"Edited\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users?size=101").cookie(session)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/invites").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"invalid\"}")).andExpect(status().isBadRequest());
    }
    @Test void deveCriarCancelarEListarConvitesComPrazoCompativel() throws Exception {
        Cookie session=login(admin);
        mvc.perform(get("/api/v1/users/options").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.roles.length()").value(4)).andExpect(jsonPath("$.teams.length()").value(5));
        Instant before=Instant.now();
        var response=mvc.perform(post("/api/v1/invites").cookie(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("email"," API-MEMBER@EXAMPLE.TEST ","roleId",role,"teamIds",List.of(root),"expiresInDays",3)))).andExpect(status().isCreated()).andReturn();
        var created=mapper.readTree(response.getResponse().getContentAsString());String id=created.path("invite").path("id").asText(),token=created.path("token").asText();
        assertThat(created.path("invite").path("email").asText()).isEqualTo("api-member@example.test");
        assertThat(Instant.parse(created.path("invite").path("expiresAt").asText())).isBetween(before.plus(Duration.ofDays(3)),Instant.now().plus(Duration.ofDays(3)));
        assertThat(created.path("invite").has("tokenHash")).isFalse();
        var listing=mvc.perform(get("/api/v1/invites").cookie(session)).andExpect(status().isOk()).andReturn();
        assertThat(listing.getResponse().getContentAsString()).doesNotContain(token,"tokenHash","inviteUrl");
        mvc.perform(post("/api/v1/invites/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("token",token,"name","Test invited","password",PASSWORD,"passwordConfirmation","Wrong confirmation")))).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email='api-member@example.test'",Long.class)).isZero();
        mvc.perform(post("/api/v1/invites/"+id+"/cancel").cookie(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/v1/invites/"+id+"/cancel").cookie(session).with(csrf())).andExpect(status().isConflict());
    }
    private Callable<Boolean> attempt(Runnable run){return ()->{try{run.run();return true;}catch(ProblemaDominio e){return false;}};}
    private String role(String key){return jdbc.queryForObject("SELECT id FROM roles WHERE role_key=?",String.class,key);}
    private String identity(String roleKey){String email=UUID.randomUUID()+"@example.test";bootstrap.criarPrimeiraIdentidade("Test identity",email,PASSWORD);String id=users.findByEmail(email).orElseThrow().getId();jdbc.update("UPDATE app_profile SET role_id=? WHERE user_id=?",role(roleKey),id);return id;}
    private ConviteService.ConviteCriadoResponse invite(String email){return invites.criarConvite(admin,email,role,List.of(root,communication));}
    private ConviteService.ConviteAceitoResponse accept(String email){return invites.aceitarConvite(invite(email).token(),"Test invited member",PASSWORD,PASSWORD);}
    private org.springframework.test.web.servlet.ResultActions loginRequest(String id) throws Exception {return mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("email",users.findById(id).orElseThrow().getEmail(),"password",PASSWORD))));}
    private Cookie login(String id) throws Exception{return loginRequest(id).andExpect(status().isOk()).andReturn().getResponse().getCookie("TS_SESSION");}
}
