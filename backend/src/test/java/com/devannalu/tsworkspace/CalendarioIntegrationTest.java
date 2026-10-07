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
class CalendarioIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_calendario_test").withLabel("com.tsworkspace.purpose", "calendario-test");
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
    @Autowired com.devannalu.tsworkspace.calendario.CalendarioService calendario;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM attachment"); jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM workspace_comment"); jdbc.update("DELETE FROM task"); jdbc.update("DELETE FROM project");
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
    private String tarefa(String equipe, LocalDate prazo, String status, boolean arquivada) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO task(id,title,status,priority,team_id,created_by_id,due_date,position,version,created_at,updated_at,archived_at) VALUES(?,? ,?,'MEDIA',?,?,?,0,0,NOW(6),NOW(6),?)",
            id, "Tarefa de calendário", status, equipe, superAdmin, prazo, arquivada ? java.sql.Timestamp.from(Instant.now()) : null);
        return id;
    }
    private String projeto(String equipe, LocalDate inicio, LocalDate fim, String status, boolean arquivado) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO project(id,title,status,team_id,created_by_id,start_date,end_date,version,created_at,updated_at,archived_at) VALUES(?,? ,?,?,?,?,?,0,NOW(6),NOW(6),?)",
            id, "Projeto de calendário", status, equipe, superAdmin, inicio, fim, arquivado ? java.sql.Timestamp.from(Instant.now()) : null);
        return id;
    }
    private static final LocalDate DE = LocalDate.of(2026,10,7), ATE = DE.plusDays(6);
    private List<com.devannalu.tsworkspace.calendario.CalendarioService.Item> listar(String pessoa) {
        return calendario.listar(pessoa, DE, ATE, null, null, null);
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
    @Test void preservaCalendarioSemTabelaPropria() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("16");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME LIKE 'calendar%'", Integer.class)).isZero();
    }
    @Test void exigeIntervaloValidoLimitadoInclusive() {
        for (LocalDate[] intervalo : new LocalDate[][]{{null,ATE},{DE,null},{ATE,DE},{DE,DE.plusDays(366)}})
            assertThatThrownBy(() -> calendario.listar(admin,intervalo[0],intervalo[1],null,null,null)).isInstanceOf(ProblemaDominio.class);
        assertThat(calendario.listar(admin,DE,DE.plusDays(365),null,null,null)).isEmpty();
    }
    @Test void tarefasUsamPrazoSemInventarDatasOuIncluirArquivadas() {
        String dentro=tarefa(equipeA,DE,"A_FAZER",false), concluida=tarefa(equipeA,ATE,"CONCLUIDA",false);
        tarefa(equipeA,null,"A_FAZER",false); tarefa(equipeA,DE.minusDays(1),"A_FAZER",false);
        tarefa(equipeA,ATE.plusDays(1),"A_FAZER",false); tarefa(equipeA,DE,"A_FAZER",true);
        var itens=listar(admin); assertThat(itens).extracting(i->i.recursoId()).containsExactlyInAnyOrder(dentro,concluida);
        assertThat(itens.get(0).dataInicio().toString()).isEqualTo("2026-10-07");
        assertThat(itens.stream().filter(i->i.recursoId().equals(concluida)).findFirst().orElseThrow().concluido()).isTrue();
    }
    @Test void projetosInterseccionamIncluemPontasEDatasIsoladas() {
        String periodo=projeto(equipeA,DE.minusDays(6),ATE.plusDays(6),"EM_ANDAMENTO",false);
        String inicio=projeto(equipeA,DE,null,"PLANEJADO",false), fim=projeto(equipeA,null,ATE,"CONCLUIDO",false);
        String limite=projeto(equipeA,ATE,ATE.plusDays(2),"PLANEJADO",false);
        projeto(equipeA,null,null,"PLANEJADO",false); projeto(equipeA,ATE.plusDays(1),ATE.plusDays(2),"PLANEJADO",false);
        projeto(equipeA,DE.minusDays(3),DE.minusDays(1),"PLANEJADO",false); projeto(equipeA,DE,ATE,"PLANEJADO",true);
        var itens=listar(admin); assertThat(itens).extracting(i->i.recursoId()).containsExactlyInAnyOrder(periodo,inicio,fim,limite);
        var pontual=itens.stream().filter(i->i.recursoId().equals(inicio)).findFirst().orElseThrow();
        assertThat(pontual.dataFim()).isEqualTo(DE);
        assertThat(itens.stream().filter(i->i.recursoId().equals(fim)).findFirst().orElseThrow().concluido()).isTrue();
    }
    @Test void calculaAtrasoDasFontesSemPersistir() {
        LocalDate ontem=LocalDate.now(ZoneId.of("America/Bahia")).minusDays(1);
        tarefa(equipeA,ontem,"A_FAZER",false); tarefa(equipeA,ontem,"CONCLUIDA",false);
        projeto(equipeA,ontem.minusDays(1),ontem,"PLANEJADO",false);
        projeto(equipeA,null,ontem,"CONCLUIDO",false); projeto(equipeA,ontem,null,"PLANEJADO",false);
        var itens=calendario.listar(admin,ontem,ontem,null,null,null);
        assertThat(itens.stream().filter(i->i.atrasado()).count()).isEqualTo(2);
        assertThat(itens.stream().filter(i->i.concluido()).allMatch(i->!i.atrasado())).isTrue();
    }
    @Test void reutilizaEscopoGlobalEEquipesParaTodosPerfis() {
        String a=tarefa(equipeA,DE,"A_FAZER",false), b=projeto(equipeB,DE,ATE,"PLANEJADO",false);
        assertThat(listar(superAdmin)).hasSize(2); assertThat(listar(admin)).hasSize(2);
        assertThat(listar(supervisora)).extracting(i->i.recursoId()).containsExactly(a);
        assertThat(listar(suporte)).extracting(i->i.recursoId()).containsExactly(a);
        assertThat(listar(fora)).extracting(i->i.recursoId()).containsExactly(b);
        assertThat(calendario.listar(suporte,DE,ATE,equipeB,null,null)).isEmpty();
    }
    @Test void permissionsParciaisENenhumaFonte() {
        tarefa(equipeA,DE,"A_FAZER",false); projeto(equipeA,DE,ATE,"PLANEJADO",false);
        negar(suporte,"projects.view"); negar(supervisora,"tasks.view");
        assertThat(listar(suporte)).allMatch(i->i.tipo().name().equals("TAREFA")).hasSize(1);
        assertThat(listar(supervisora)).allMatch(i->i.tipo().name().equals("PROJETO")).hasSize(1);
        negar(suporte,"tasks.view"); negar(suporte,"meetings.view"); negar(suporte,"events.view"); negar(suporte,"content.view"); assertThatThrownBy(()->listar(suporte)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void filtrosEMesmoUuidEntreFontes() {
        String t=tarefa(equipeA,DE,"A_FAZER",false), p=projeto(equipeA,DE,ATE,"PLANEJADO",false);
        jdbc.update("UPDATE project SET id=? WHERE id=?",t,p);
        jdbc.update("INSERT INTO task_assignee(task_id,user_id) VALUES(?,?)",t,suporte);
        jdbc.update("INSERT INTO project_responsible(project_id,user_id) VALUES(?,?)",t,supervisora);
        assertThat(listar(admin)).extracting(i->i.id()).doesNotHaveDuplicates().containsExactlyInAnyOrder("TAREFA:"+t,"PROJETO:"+t);
        var tipo=Set.of(com.devannalu.tsworkspace.calendario.CalendarioService.Tipo.PROJETO);
        assertThat(calendario.listar(admin,DE,ATE,equipeA,tipo,supervisora)).hasSize(1);
        assertThat(calendario.listar(admin,DE,ATE,equipeA,tipo,suporte)).isEmpty();
        assertThat(calendario.listar(admin,DE,ATE,equipeA,null,suporte)).hasSize(1);
    }
    @Test void httpExigeSessaoDatasEPermissionPreservaDateOnly() throws Exception {
        tarefa(equipeA,DE,"A_FAZER",false);
        http.perform(get("/api/v1/calendar").param("from",DE.toString()).param("to",ATE.toString())).andExpect(status().isUnauthorized());
        var sessao=entrar(suporte);
        http.perform(get("/api/v1/calendar").cookie(sessao)).andExpect(status().isBadRequest());
        for (String data : List.of("invalid","2026-02-30")) http.perform(get("/api/v1/calendar").cookie(sessao).param("from",data).param("to",ATE.toString())).andExpect(status().isBadRequest());
        http.perform(get("/api/v1/calendar").cookie(sessao).param("from",DE.toString()).param("to",DE.plusDays(366).toString()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Informe um intervalo válido de até 366 dias."));
        http.perform(get("/api/v1/calendar").cookie(sessao).param("from",DE.toString()).param("to",ATE.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].dataInicio").value("2026-10-07"));
        negar(suporte,"tasks.view"); negar(suporte,"projects.view"); negar(suporte,"meetings.view"); negar(suporte,"events.view"); negar(suporte,"content.view");
        http.perform(get("/api/v1/calendar").cookie(sessao).param("from",DE.toString()).param("to",ATE.toString())).andExpect(status().isForbidden());
    }
}
