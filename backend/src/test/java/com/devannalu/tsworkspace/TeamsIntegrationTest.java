package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.auth.*;
import com.devannalu.tsworkspace.teams.*;
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
class TeamsIntegrationTest {
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
    @Autowired TeamService teams;
    @Autowired TeamSeed seed;
    @Autowired TeamController controller;
    @Autowired BootstrapService bootstrap;
    @Autowired UserRepository users;
    @Autowired AppUserDetailsService details;
    @Autowired Flyway flyway;
    private String admin, root;
    private static final String PASSWORD = "Teams integration test password";
    @BeforeEach void reset() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM team_member"); jdbc.update("DELETE FROM user_permissions");
        jdbc.update("DELETE FROM app_profile"); jdbc.update("DELETE FROM app_user");
        // Delete only the ephemeral test tree, leaves before ancestors.
        while (jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE team_key<>'fundadoras'",Long.class)>0)
            jdbc.update("DELETE FROM team WHERE id IN (SELECT id FROM (SELECT t.id FROM team t LEFT JOIN team c ON c.parent_id=t.id WHERE t.team_key<>'fundadoras' AND c.id IS NULL) leaves)");
        seed.seed(); admin = user("SUPER_ADMIN"); seed.seed();
        root = jdbc.queryForObject("SELECT id FROM team WHERE team_key='fundadoras'",String.class);
    }
    @Test void fullMigrationSeedAndFrozenPrismaCompatibility() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        seed.seed(); seed.seed();
        try(var input=getClass().getResourceAsStream("/teams-baseline.json")) {
            JsonNode baseline=mapper.readTree(input);
            assertThat(teams.list()).hasSize(5);
            var rootTeam=teams.detail(root).team();
            assertThat(rootTeam.key()).isEqualTo(baseline.get("root").get("key").asText());
            assertThat(rootTeam.name()).isEqualTo(baseline.get("root").get("name").asText());
            assertThat(rootTeam.parentId()).isNull();
            var children=baseline.get("children").fields();
            while(children.hasNext()) {
                var entry=children.next();
                var team=teams.list().stream().filter(t->t.key().equals(entry.getKey())).findFirst().orElseThrow();
                assertThat(team.name()).isEqualTo(entry.getValue().asText()); assertThat(team.parentId()).isEqualTo(root);
            }
            var tree=List.of(new TeamPolicy.Node("root","fundadoras",null,false),new TeamPolicy.Node("a","a","root",false),new TeamPolicy.Node("b","b","a",false),new TeamPolicy.Node("c","c","b",false),new TeamPolicy.Node("archived","archived","root",true));
            for(var test:baseline.get("parentCases")) {
                String parent=test.get("parent").isNull()?null:test.get("parent").asText();
                boolean allowed;
                try { TeamPolicy.parent(tree,test.get("id").asText(),parent); allowed=true; } catch(TeamProblem e) { allowed=false; }
                assertThat(allowed).isEqualTo(test.get("allowed").asBoolean());
            }
        }
        assertThat(teams.detail(root).members()).extracting(TeamService.Member::id).containsExactly(admin);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user",Long.class)).isEqualTo(1);
    }
    @Test void createEditListDetailAndArchiveWithHistory() {
        String id=teams.create("Teste","description",root).team().id();
        String child=teams.create("Child",null,id).team().id();
        assertThatThrownBy(()->teams.archive(id)).hasMessageContaining("subequipes");
        teams.edit(child,"Moved",null,root);
        teams.addMember(id,admin);
        assertThat(teams.detail(id).team().memberCount()).isEqualTo(1);
        assertThat(teams.archive(id).team().archived()).isTrue();
        assertThat(teams.archive(id).team().archived()).isTrue();
        assertThat(teams.list()).extracting(TeamService.Summary::id).doesNotContain(id);
        assertThat(teams.detail(id).members()).hasSize(1);
        assertThatThrownBy(()->teams.edit(id,"No",null,root)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.addMember(id,admin)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.removeMember(id,admin)).isInstanceOf(TeamProblem.class);
    }
    @Test void seedPreservesAdministrativeChangesAndRejectsInvalidRoot() {
        String events=jdbc.queryForObject("SELECT id FROM team WHERE team_key='eventos'",String.class);
        teams.edit(events,"Eventos personalizados","Preserved",root);
        teams.archive(events);
        String custom=teams.create("Custom team",null,root).team().id();
        seed.seed(); seed.seed();
        assertThat(teams.detail(events).team().name()).isEqualTo("Eventos personalizados");
        assertThat(teams.detail(events).team().archived()).isTrue();
        assertThat(teams.detail(custom).team().name()).isEqualTo("Custom team");
        jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP(6) WHERE id=?",root);
        try { assertThatThrownBy(()->seed.seed()).hasMessageContaining("estado inválido"); }
        finally { jdbc.update("UPDATE team SET archived_at=NULL WHERE id=?",root); }
    }
    @Test void hierarchyAndRootProtections() {
        String a=teams.create("Parent",null,root).team().id(), b=teams.create("Child",null,a).team().id(), c=teams.create("Grandchild",null,b).team().id();
        assertThatThrownBy(()->teams.create("Second root",null,null)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.edit(a,"Parent",null,a)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.edit(a,"Parent",null,b)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.edit(a,"Parent",null,c)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.edit(a,"Parent",null,null)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.edit(root,"Fundadoras",null,a)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.archive(root)).isInstanceOf(TeamProblem.class);
        teams.archive(c);
        assertThatThrownBy(()->teams.create("Invalid",null,c)).isInstanceOf(TeamProblem.class);
        assertThatThrownBy(()->teams.create("Invalid",null,UUID.randomUUID().toString())).isInstanceOfSatisfying(TeamProblem.class,e->assertThat(e.status()).isEqualTo(404));
    }
    @Test void manyToManyDuplicateInactiveMembersAndLastAdmin() {
        String other=user("SUPPORT"), a=teams.create("A team",null,root).team().id(), b=teams.create("B team",null,root).team().id();
        teams.addMember(a,other); teams.addMember(b,other);
        assertThatThrownBy(()->teams.addMember(a,other)).hasMessageContaining("já");
        teams.removeMember(a,other); assertThat(teams.detail(b).members()).hasSize(1);
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?",other);
        assertThat(teams.detail(b).team().memberCount()).isZero(); assertThat(teams.detail(b).members()).isEmpty();
        assertThatThrownBy(()->teams.addMember(a,other)).hasMessageContaining("indisponível");
        teams.removeMember(b,other); // inactive existing members may be removed, as in Prisma
        assertThatThrownBy(()->teams.removeMember(root,admin)).hasMessageContaining("última");
        String second=user("SUPER_ADMIN"); teams.addMember(root,second); teams.removeMember(root,admin);
        assertThatThrownBy(()->teams.removeMember(root,second)).hasMessageContaining("última");
    }
    @Test void databaseConstraintsRejectDuplicateMembershipSecondRootAndMissingForeignKeys() {
        assertThatThrownBy(()->jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))",admin,root)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO team (id,team_key,name,created_at,updated_at) VALUES (?,'second-root','Second',CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",UUID.randomUUID().toString())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))",UUID.randomUUID().toString(),root)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void concurrentMembershipAndHierarchyMutationsSerialize() throws Exception {
        String member=user("SUPPORT"), a=teams.create("Concurrent A",null,root).team().id(), b=teams.create("Concurrent B",null,root).team().id();
        var pool=Executors.newFixedThreadPool(2);
        try {
            var duplicate=pool.invokeAll(List.of(attempt(()->teams.addMember(a,member)),attempt(()->teams.addMember(a,member))));
            assertThat(List.of(duplicate.get(0).get(),duplicate.get(1).get())).containsExactlyInAnyOrder(true,false);
            var cycle=pool.invokeAll(List.of(attempt(()->teams.edit(a,"Concurrent A",null,b)),attempt(()->teams.edit(b,"Concurrent B",null,a))));
            assertThat(List.of(cycle.get(0).get(),cycle.get(1).get())).containsExactlyInAnyOrder(true,false);
            String second=user("SUPER_ADMIN"); teams.addMember(root,second);
            var removal=pool.invokeAll(List.of(attempt(()->teams.removeMember(root,admin)),attempt(()->teams.removeMember(root,second))));
            assertThat(List.of(removal.get(0).get(),removal.get(1).get())).containsExactlyInAnyOrder(true,false);
        } finally { pool.shutdownNow(); }
    }
    private Callable<Boolean> attempt(Runnable run) { return () -> { try { run.run(); return true; } catch(TeamProblem e) { return false; } }; }
    @Test void apiRbacValidationErrorsCsrfAndCrud() throws Exception {
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
    @Test void eachPermissionAndMethodSecurityAreEnforced() throws Exception {
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
        try { assertThatThrownBy(()->controller.list()).isInstanceOf(AccessDeniedException.class); }
        finally { SecurityContextHolder.clearContext(); }
    }
    private String user(String role) {
        String email=UUID.randomUUID()+"@example.test";
        bootstrap.createFirstIdentity("Teams Test",email,PASSWORD);
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
