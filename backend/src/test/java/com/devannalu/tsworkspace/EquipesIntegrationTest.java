package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.auth.*;
import com.devannalu.tsworkspace.usuarios.*;
import com.devannalu.tsworkspace.autenticacao.*;
import com.devannalu.tsworkspace.equipes.*;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EquipesIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_teams_test").withLabel("com.tsworkspace.purpose","teams-test");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EquipeService teams;
    @Autowired InicializacaoEquipesService seed;
    @Autowired EquipeController controller;
    @Autowired InicializacaoIdentidadeService bootstrap;
    @Autowired UsuarioRepository users;
    @Autowired AutenticacaoUsuarioService details;
    @Autowired Flyway flyway;
    private String admin, root;
    private static final String PASSWORD = "Teams integration test password";
    @BeforeEach void reset() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM team_member"); jdbc.update("DELETE FROM user_permissions");
        jdbc.update("DELETE FROM app_profile"); jdbc.update("DELETE FROM app_user");
        // Apenas a árvore temporária é removida, das folhas para a raiz.
        while (jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE team_key<>'fundadoras'",Long.class)>0)
            jdbc.update("DELETE FROM team WHERE id IN (SELECT id FROM (SELECT t.id FROM team t LEFT JOIN team c ON c.parent_id=t.id WHERE t.team_key<>'fundadoras' AND c.id IS NULL) leaves)");
        seed.seed(); admin = user("SUPER_ADMIN"); seed.seed();
        root = jdbc.queryForObject("SELECT id FROM team WHERE team_key='fundadoras'",String.class);
    }
    @Test void devePreservarInicializacaoECompatibilidadeHistoricaDasEquipes() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("11");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        seed.seed(); seed.seed();
        try(var input=getClass().getResourceAsStream("/teams-baseline.json")) {
            JsonNode baseline=mapper.readTree(input);
            assertThat(teams.listarEquipes()).hasSize(5);
            var rootTeam=teams.buscarDetalheEquipe(root).team();
            assertThat(rootTeam.key()).isEqualTo(baseline.get("root").get("key").asText());
            assertThat(rootTeam.name()).isEqualTo(baseline.get("root").get("name").asText());
            assertThat(rootTeam.parentId()).isNull();
            var children=baseline.get("children").fields();
            while(children.hasNext()) {
                var entry=children.next();
                var team=teams.listarEquipes().stream().filter(t->t.key().equals(entry.getKey())).findFirst().orElseThrow();
                assertThat(team.name()).isEqualTo(entry.getValue().asText()); assertThat(team.parentId()).isEqualTo(root);
            }
            var tree=List.of(new PoliticaEquipe.NoHierarquia("root","fundadoras",null,false),new PoliticaEquipe.NoHierarquia("a","a","root",false),new PoliticaEquipe.NoHierarquia("b","b","a",false),new PoliticaEquipe.NoHierarquia("c","c","b",false),new PoliticaEquipe.NoHierarquia("archived","archived","root",true));
            for(var test:baseline.get("parentCases")) {
                String parent=test.get("parent").isNull()?null:test.get("parent").asText();
                boolean allowed;
                try { PoliticaEquipe.validarEquipeMae(tree,test.get("id").asText(),parent); allowed=true; } catch(ProblemaDominio e) { allowed=false; }
                assertThat(allowed).isEqualTo(test.get("allowed").asBoolean());
            }
        }
        assertThat(teams.buscarDetalheEquipe(root).members()).extracting(EquipeService.IntegranteEquipe::id).containsExactly(admin);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user",Long.class)).isEqualTo(1);
    }
    @Test void deveListarEquipesPropriasRespeitandoParticipacaoEPermissao() throws Exception {
        String support = user("SUPPORT");
        String own = teams.criarEquipe("Own team", null, root).team().id();
        String archived = teams.criarEquipe("Archived team", null, root).team().id();
        teams.adicionarIntegrante(own, support); teams.adicionarIntegrante(archived, support); teams.arquivarEquipe(archived);
        Cookie session = login(support);
        mvc.perform(get("/api/v1/teams/mine").cookie(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(own));
        mvc.perform(get("/api/v1/teams/mine")).andExpect(status().isUnauthorized());
        jdbc.update("INSERT INTO user_permissions SELECT ?,id,'DENY' FROM permissions WHERE permission_key='teams.view'", support);
        mvc.perform(get("/api/v1/teams/mine").cookie(session)).andExpect(status().isForbidden());
    }
    @Test void deveCriarEditarConsultarEArquivarEquipePreservandoHistorico() {
        String id=teams.criarEquipe("Teste","description",root).team().id();
        String child=teams.criarEquipe("Child",null,id).team().id();
        assertThatThrownBy(()->teams.arquivarEquipe(id)).hasMessageContaining("subequipes");
        teams.editarEquipe(child,"Moved",null,root);
        teams.adicionarIntegrante(id,admin);
        assertThat(teams.buscarDetalheEquipe(id).team().memberCount()).isEqualTo(1);
        assertThat(teams.arquivarEquipe(id).team().archived()).isTrue();
        assertThat(teams.arquivarEquipe(id).team().archived()).isTrue();
        assertThat(teams.listarEquipes()).extracting(EquipeService.ResumoEquipe::id).doesNotContain(id);
        assertThat(teams.buscarDetalheEquipe(id).members()).hasSize(1);
        assertThatThrownBy(()->teams.editarEquipe(id,"No",null,root)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.adicionarIntegrante(id,admin)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.removerIntegrante(id,admin)).isInstanceOf(ProblemaDominio.class);
    }
    @Test void devePreservarAlteracoesAdministrativasERejeitarRaizInvalida() {
        String events=jdbc.queryForObject("SELECT id FROM team WHERE team_key='eventos'",String.class);
        teams.editarEquipe(events,"Eventos personalizados","Preserved",root);
        teams.arquivarEquipe(events);
        String custom=teams.criarEquipe("Custom team",null,root).team().id();
        seed.seed(); seed.seed();
        assertThat(teams.buscarDetalheEquipe(events).team().name()).isEqualTo("Eventos personalizados");
        assertThat(teams.buscarDetalheEquipe(events).team().archived()).isTrue();
        assertThat(teams.buscarDetalheEquipe(custom).team().name()).isEqualTo("Custom team");
        jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP(6) WHERE id=?",root);
        try { assertThatThrownBy(()->seed.seed()).hasMessageContaining("estado inválido"); }
        finally { jdbc.update("UPDATE team SET archived_at=NULL WHERE id=?",root); }
    }
    @Test void deveProtegerHierarquiaERaiz() {
        String a=teams.criarEquipe("Parent",null,root).team().id(), b=teams.criarEquipe("Child",null,a).team().id(), c=teams.criarEquipe("Grandchild",null,b).team().id();
        assertThatThrownBy(()->teams.criarEquipe("Second root",null,null)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.editarEquipe(a,"Parent",null,a)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.editarEquipe(a,"Parent",null,b)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.editarEquipe(a,"Parent",null,c)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.editarEquipe(a,"Parent",null,null)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.editarEquipe(root,"Fundadoras",null,a)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.arquivarEquipe(root)).isInstanceOf(ProblemaDominio.class);
        teams.arquivarEquipe(c);
        assertThatThrownBy(()->teams.criarEquipe("Invalid",null,c)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->teams.criarEquipe("Invalid",null,UUID.randomUUID().toString())).isInstanceOfSatisfying(ProblemaDominio.class,e->assertThat(e.status()).isEqualTo(404));
    }
    @Test void deveValidarParticipacoesDuplicidadeInatividadeEUltimaAdmin() {
        String other=user("SUPPORT"), a=teams.criarEquipe("A team",null,root).team().id(), b=teams.criarEquipe("B team",null,root).team().id();
        teams.adicionarIntegrante(a,other); teams.adicionarIntegrante(b,other);
        assertThatThrownBy(()->teams.adicionarIntegrante(a,other)).hasMessageContaining("já");
        teams.removerIntegrante(a,other); assertThat(teams.buscarDetalheEquipe(b).members()).hasSize(1);
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?",other);
        assertThat(teams.buscarDetalheEquipe(b).team().memberCount()).isZero(); assertThat(teams.buscarDetalheEquipe(b).members()).isEmpty();
        assertThatThrownBy(()->teams.adicionarIntegrante(a,other)).hasMessageContaining("indisponível");
        teams.removerIntegrante(b,other); // Remover integrantes inativas preserva a compatibilidade do fluxo.
        assertThatThrownBy(()->teams.removerIntegrante(root,admin)).hasMessageContaining("última");
        String second=user("SUPER_ADMIN"); teams.adicionarIntegrante(root,second); teams.removerIntegrante(root,admin);
        assertThatThrownBy(()->teams.removerIntegrante(root,second)).hasMessageContaining("última");
    }
    @Test void deveImpedirParticipacaoDuplicadaSegundaRaizEReferenciaInexistente() {
        assertThatThrownBy(()->jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))",admin,root)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO team (id,team_key,name,created_at,updated_at) VALUES (?,'second-root','Second',CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",UUID.randomUUID().toString())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))",UUID.randomUUID().toString(),root)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void deveSerializarAlteracoesConcorrentesDeParticipacaoEHierarquia() throws Exception {
        String member=user("SUPPORT"), a=teams.criarEquipe("Concurrent A",null,root).team().id(), b=teams.criarEquipe("Concurrent B",null,root).team().id();
        var pool=Executors.newFixedThreadPool(2);
        try {
            var duplicate=pool.invokeAll(List.of(attempt(()->teams.adicionarIntegrante(a,member)),attempt(()->teams.adicionarIntegrante(a,member))));
            assertThat(List.of(duplicate.get(0).get(),duplicate.get(1).get())).containsExactlyInAnyOrder(true,false);
            var cycle=pool.invokeAll(List.of(attempt(()->teams.editarEquipe(a,"Concurrent A",null,b)),attempt(()->teams.editarEquipe(b,"Concurrent B",null,a))));
            assertThat(List.of(cycle.get(0).get(),cycle.get(1).get())).containsExactlyInAnyOrder(true,false);
            String second=user("SUPER_ADMIN"); teams.adicionarIntegrante(root,second);
            var removal=pool.invokeAll(List.of(attempt(()->teams.removerIntegrante(root,admin)),attempt(()->teams.removerIntegrante(root,second))));
            assertThat(List.of(removal.get(0).get(),removal.get(1).get())).containsExactlyInAnyOrder(true,false);
        } finally { pool.shutdownNow(); }
    }
    private Callable<Boolean> attempt(Runnable run) { return () -> { try { run.run(); return true; } catch(ProblemaDominio e) { return false; } }; }
    @Test void devePreservarAutorizacaoValidacaoCsrfEOperacoesDasEquipes() throws Exception {
        mvc.perform(get("/api/v1/teams")).andExpect(status().isUnauthorized());
        Cookie session=login(admin);
        mvc.perform(get("/api/v1/teams").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/v1/teams/"+root).cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.members[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/teams/"+UUID.randomUUID()).cookie(session)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/teams/invalid").cookie(session)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/teams").cookie(session).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/teams").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" a \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/teams").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Second root\"}")).andExpect(status().isConflict());
        var result=mvc.perform(post("/api/v1/teams").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("name","API Team","parentId",root)))).andExpect(status().isCreated()).andReturn();
        String id=mapper.readTree(result.getResponse().getContentAsString()).get("team").get("id").asText();
        mvc.perform(put("/api/v1/teams/"+id).cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("name","Edited API","parentId",root)))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/teams/"+id+"/members").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("userId",admin)))).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/teams/"+id+"/members").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("userId",admin)))).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/teams/"+id+"/members/"+admin).cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/teams/"+id+"/archive").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/teams/"+root+"/archive").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/teams/"+root).cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isForbidden());
    }
    @Test void deveExigirCadaPermissaoEProtegerMetodos() throws Exception {
        String support=user("SUPPORT"); Cookie session=login(support);
        jdbc.update("INSERT INTO user_permissions SELECT ?,id,'DENY' FROM permissions WHERE permission_key='teams.view'",support);
        mvc.perform(get("/api/v1/teams").cookie(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/teams/"+root).cookie(session)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/teams").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("name","Forbidden","parentId",root)))).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/teams/"+root).cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("name","Forbidden","parentId",root)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/teams/"+root+"/archive").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/teams/"+root+"/members").cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("userId",support)))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/teams/"+root+"/members/"+support).cookie(session).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isForbidden());
        var principal=details.loadUserByUsername(users.findById(support).orElseThrow().getEmail());
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
        try { assertThatThrownBy(()->controller.listarEquipes()).isInstanceOf(AccessDeniedException.class); }
        finally { SecurityContextHolder.clearContext(); }
    }
    private String user(String role) {
        String email=UUID.randomUUID()+"@example.test";
        bootstrap.criarPrimeiraIdentidade("Teams Test",email,PASSWORD);
        String id=users.findByEmail(email).orElseThrow().getId();
        jdbc.update("UPDATE app_profile SET role_id=(SELECT id FROM roles WHERE role_key=?) WHERE user_id=?",role,id);
        return id;
    }
    private Cookie login(String id) throws Exception {
        var result=mvc.perform(post("/api/v1/auth/login").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("email",users.findById(id).orElseThrow().getEmail(),"password",PASSWORD)))).andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie("TS_SESSION");
    }
}
