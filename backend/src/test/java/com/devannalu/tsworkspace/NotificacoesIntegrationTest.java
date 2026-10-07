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
class NotificacoesIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_notificacoes_test").withLabel("com.tsworkspace.purpose", "notificacoes-test");
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
    @Autowired com.devannalu.tsworkspace.notificacoes.NotificacaoService notificacoes;
    @Autowired com.devannalu.tsworkspace.tarefas.TarefaService tarefas;
    @Autowired com.devannalu.tsworkspace.projetos.ProjetoService projetos;
    @Autowired com.devannalu.tsworkspace.comentarios.ComentarioService comentarios;
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
    private void negar(String pessoa, String permissao) {
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key=?", pessoa, permissao);
    }
    private Cookie entrar(String usuario) throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM app_user WHERE id=?", String.class, usuario);
        return http.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", SENHA_TESTE))))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("TS_SESSION");
    }
    private com.devannalu.tsworkspace.tarefas.TarefaService.TarefaResponse criarTarefa(List<String> pessoas) {
        return tarefas.criarTarefa(superAdmin,"Aviso de tarefa",null,null,equipeA,null,pessoas);
    }
    @Test void atribuicaoIgnoraAutoraENaoRepeteResponsavelAnterior() {
        vincular(superAdmin,equipeA);
        var tarefa=criarTarefa(List.of(suporte,superAdmin));
        assertThat(notificacoes.listar(superAdmin,0,20).total()).isZero();
        assertThat(notificacoes.listar(suporte,0,20).items()).singleElement().satisfies(n->{assertThat(n.motivo()).isEqualTo("ATRIBUICAO");assertThat(n.titulo()).isEqualTo(tarefa.titulo());});
        tarefas.editarTarefa(superAdmin,tarefa.id(),tarefa.titulo(),"Descrição revisada",tarefa.prioridade(),equipeA,null,List.of(suporte,superAdmin),tarefa.versao());
        assertThat(notificacoes.listar(suporte,0,20).total()).isEqualTo(1);
    }
    @Test void novaResponsavelComentarioEStatusAvisamSemTextoPrivado() {
        var tarefa=criarTarefa(List.of(suporte));
        var nova=tarefas.editarTarefa(superAdmin,tarefa.id(),tarefa.titulo(),null,tarefa.prioridade(),equipeA,null,List.of(suporte,outra),tarefa.versao());
        assertThat(notificacoes.listar(outra,0,20).items()).singleElement().satisfies(n->assertThat(n.motivo()).isEqualTo("ATRIBUICAO"));
        comentarios.criar(superAdmin,com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso.TAREFA,tarefa.id(),"Comentário relevante sem expor o conteúdo no aviso");
        tarefas.moverTarefa(superAdmin,tarefa.id(),com.devannalu.tsworkspace.tarefas.Tarefa.Status.EM_ANDAMENTO,null,nova.versao());
        assertThat(notificacoes.listar(suporte,0,20).items()).extracting(n->n.motivo()).containsExactlyInAnyOrder("ATRIBUICAO","COMENTARIO","ALTERACAO");
    }
    @Test void responsabilidadeDeProjetoENovaLeitura() {
        var projeto=projetos.criarProjeto(superAdmin,"Aviso de projeto",null,equipeA,List.of(supervisora),null,null);
        var pagina=notificacoes.listar(supervisora,0,20);
        assertThat(pagina.naoLidas()).isEqualTo(1);assertThat(pagina.items().get(0).tipo()).isEqualTo("PROJETO");
        notificacoes.marcarLida(supervisora,pagina.items().get(0).id());notificacoes.marcarLida(supervisora,pagina.items().get(0).id());
        assertThat(notificacoes.listar(supervisora,0,20).naoLidas()).isZero();
        assertThatThrownBy(()->notificacoes.marcarLida(suporte,pagina.items().get(0).id())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void revogacaoDePermissionOuEquipeOcultaAvisosETituloAtual() {
        var tarefa=criarTarefa(List.of(suporte));
        negar(suporte,"tasks.view");assertThat(notificacoes.listar(suporte,0,20).items()).isEmpty();
        jdbc.update("DELETE FROM user_permissions WHERE user_id=?",suporte);
        jdbc.update("DELETE FROM team_member WHERE user_id=?",suporte);assertThat(notificacoes.listar(suporte,0,20).total()).isZero();
        vincular(suporte,equipeA);jdbc.update("UPDATE task SET title='Título atual' WHERE id=?",tarefa.id());
        assertThat(notificacoes.listar(suporte,0,20).items().get(0).titulo()).isEqualTo("Título atual");
        tarefas.arquivarTarefa(superAdmin,tarefa.id(),tarefa.versao());assertThat(notificacoes.listar(suporte,0,20).items()).isEmpty();
    }
    @Test void prazoEAtrasoSaoDeduplicadosEConcluidaNaoGeraAviso() {
        LocalDate hoje=LocalDate.now(ZoneId.of("America/Bahia"));
        String atrasada=tarefa(equipeA,hoje.minusDays(1),"A_FAZER",false);
        String proxima=tarefa(equipeA,hoje.plusDays(2),"A_FAZER",false);
        String concluida=tarefa(equipeA,hoje,"CONCLUIDA",false);
        for(String id:List.of(atrasada,proxima,concluida))jdbc.update("INSERT INTO task_assignee(task_id,user_id) VALUES(?,?)",id,suporte);
        assertThat(notificacoes.listar(suporte,0,20).items()).extracting(n->n.motivo()).containsExactlyInAnyOrder("ATRASO","PRAZO");
        assertThat(notificacoes.listar(suporte,0,20).total()).isEqualTo(2);
        notificacoes.marcarLida(suporte,null);assertThat(notificacoes.listar(suporte,0,20).naoLidas()).isZero();
    }
    @Test void paginaLimitesEHttpExigemSessaoCsrfEAutoria() throws Exception {
        for(int i=0;i<3;i++)criarTarefa(List.of(suporte));
        assertThat(notificacoes.listar(suporte,1,2).items()).hasSize(1);
        assertThatThrownBy(()->notificacoes.listar(suporte,-1,20)).isInstanceOf(IllegalArgumentException.class);
        http.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        var sessao=entrar(suporte);var id=notificacoes.listar(suporte,0,20).items().get(0).id();
        http.perform(post("/api/v1/notifications/"+id+"/read").cookie(sessao)).andExpect(status().isForbidden());
        http.perform(post("/api/v1/notifications/"+id+"/read").cookie(sessao).with(csrf())).andExpect(status().isNoContent());
        http.perform(post("/api/v1/notifications/"+UUID.randomUUID()+"/read").cookie(sessao).with(csrf())).andExpect(status().isNotFound());
        http.perform(get("/api/v1/notifications").cookie(sessao).param("size","101")).andExpect(status().isBadRequest());
        http.perform(get("/api/v1/notifications").cookie(sessao)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3));
    }
}
