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
class ChecklistIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_checklist_test").withLabel("com.tsworkspace.purpose", "checklist-test");
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
    @Autowired com.devannalu.tsworkspace.tarefas.ChecklistService checklist;
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
    @Test void itensTemAutoriaProgressoTextoSeguroERemocaoLogica() {
        var tarefa=criarTarefa(List.of(suporte));
        var primeira=checklist.criar(suporte,tarefa.id(),"  Preparar pauta  ");
        var item=primeira.items().get(0);
        assertThat(item.texto()).isEqualTo("Preparar pauta");assertThat(item.criadaPor().id()).isEqualTo(suporte);
        var segunda=checklist.criar(superAdmin,tarefa.id(),"Conferir materiais");
        assertThat(segunda.total()).isEqualTo(2);assertThat(segunda.concluidos()).isZero();
        var concluida=checklist.editar(suporte,tarefa.id(),item.id(),"<script>texto simples</script>",true,item.versao());
        assertThat(concluida.concluidos()).isEqualTo(1);
        var atual=concluida.items().get(0);
        assertThat(atual.versao()).isEqualTo(1);assertThat(atual.texto()).contains("<script>");
        var reaberta=checklist.editar(suporte,tarefa.id(),atual.id(),atual.texto(),false,atual.versao());
        assertThat(reaberta.concluidos()).isZero();
        checklist.remover(suporte,tarefa.id(),atual.id(),reaberta.items().get(0).versao());
        assertThat(checklist.listar(suporte,tarefa.id()).total()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM task_checklist WHERE id=?",Boolean.class,item.id())).isTrue();
    }
    @Test void ordenacaoExigeConjuntoCompletoEVersionado() {
        var tarefa=criarTarefa(List.of(suporte));checklist.criar(superAdmin,tarefa.id(),"Primeiro");
        var lista=checklist.criar(superAdmin,tarefa.id(),"Segundo");var primeiro=lista.items().get(0);var segundo=lista.items().get(1);
        var invertida=checklist.ordenar(suporte,tarefa.id(),List.of(new com.devannalu.tsworkspace.tarefas.ChecklistService.Posicao(segundo.id(),segundo.versao()),new com.devannalu.tsworkspace.tarefas.ChecklistService.Posicao(primeiro.id(),primeiro.versao())));
        assertThat(invertida.items()).extracting(i->i.texto()).containsExactly("Segundo","Primeiro");
        assertThatThrownBy(()->checklist.ordenar(suporte,tarefa.id(),List.of(new com.devannalu.tsworkspace.tarefas.ChecklistService.Posicao(segundo.id(),segundo.versao()),new com.devannalu.tsworkspace.tarefas.ChecklistService.Posicao(primeiro.id(),primeiro.versao())))).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->checklist.ordenar(suporte,tarefa.id(),List.of())).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->checklist.editar(suporte,tarefa.id(),primeiro.id(),"Não sobrescrever",false,0)).isInstanceOf(ProblemaDominio.class);
    }
    @Test void acessoHerdaTarefaEquipeArquivoEPermissionAtual() {
        var tarefa=criarTarefa(List.of(suporte));
        assertThat(checklist.listar(outra,tarefa.id()).podeEditar()).isFalse();
        assertThatThrownBy(()->checklist.criar(outra,tarefa.id(),"Não autorizado")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->checklist.listar(fora,tarefa.id())).isInstanceOf(AccessDeniedException.class);
        negar(suporte,"tasks.edit");assertThatThrownBy(()->checklist.criar(suporte,tarefa.id(),"Sem permissão")).isInstanceOf(AccessDeniedException.class);
        tarefas.arquivarTarefa(superAdmin,tarefa.id(),tarefa.versao());
        assertThat(checklist.listar(superAdmin,tarefa.id()).podeEditar()).isFalse();
        assertThatThrownBy(()->checklist.criar(superAdmin,tarefa.id(),"Arquivada")).isInstanceOf(AccessDeniedException.class);
    }
    @Test void itemDeOutraTarefaNaoPodeSerAlteradoETextoTemLimite() {
        var a=criarTarefa(List.of(suporte));var b=criarTarefa(List.of(suporte));var item=checklist.criar(superAdmin,a.id(),"Item restrito").items().get(0);
        assertThatThrownBy(()->checklist.editar(suporte,b.id(),item.id(),"Troca indevida",true,0)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->checklist.criar(suporte,a.id()," ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->checklist.criar(suporte,a.id(),"x".repeat(501))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->checklist.criar(superAdmin,UUID.randomUUID().toString(),"Ausente")).isInstanceOf(ProblemaDominio.class);
    }
    @Test void httpExigeSessaoCsrfVersaoEValidacao() throws Exception {
        var tarefa=criarTarefa(List.of(suporte));String rota="/api/v1/tasks/"+tarefa.id()+"/checklist";
        http.perform(get(rota)).andExpect(status().isUnauthorized());var cookie=entrar(suporte);
        http.perform(post(rota).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Item\"}")).andExpect(status().isForbidden());
        http.perform(post(rota).cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Item\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(1));
        var item=checklist.listar(suporte,tarefa.id()).items().get(0);
        http.perform(patch(rota+"/"+item.id()).cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Novo\",\"concluido\":true,\"versao\":99}")).andExpect(status().isConflict());
        http.perform(post(rota).cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\" \"}")).andExpect(status().isBadRequest());
        http.perform(get(rota).cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.podeEditar").value(true));
    }
    @Test void duasEdicoesConcorrentesNaoSobrescrevemItem() throws Exception {
        var tarefa=criarTarefa(List.of(suporte));var item=checklist.criar(superAdmin,tarefa.id(),"Original").items().get(0);
        var inicio=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var resultados=new ArrayList<java.util.concurrent.Future<Boolean>>();
            for(String texto:List.of("Primeira edição","Segunda edição"))resultados.add(pool.submit(()->{
                inicio.await();try {checklist.editar(suporte,tarefa.id(),item.id(),texto,true,0);return true;}
                catch(ProblemaDominio conflito){assertThat(conflito.status()).isEqualTo(409);return false;}
            }));
            inicio.countDown();int sucessos=0;for(var resultado:resultados)if(resultado.get(15,java.util.concurrent.TimeUnit.SECONDS))sucessos++;
            assertThat(sucessos).isEqualTo(1);assertThat(checklist.listar(suporte,tarefa.id()).items().get(0).versao()).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    @Test void limiteDeItensEDuplicacaoDeOrdemSaoRejeitados() {
        var tarefa=criarTarefa(List.of(suporte));
        for(int i=0;i<200;i++)jdbc.update("INSERT INTO task_checklist(id,task_id,text,position,created_by_id,created_at,updated_at) VALUES(?,?,?,?,?,NOW(6),NOW(6))",UUID.randomUUID().toString(),tarefa.id(),"Item "+i,i,superAdmin);
        assertThatThrownBy(()->checklist.criar(suporte,tarefa.id(),"Excedente")).isInstanceOf(ProblemaDominio.class);
        var item=checklist.listar(suporte,tarefa.id()).items().get(0);var posicao=new com.devannalu.tsworkspace.tarefas.ChecklistService.Posicao(item.id(),item.versao());
        assertThatThrownBy(()->checklist.ordenar(suporte,tarefa.id(),List.of(posicao,posicao))).isInstanceOf(IllegalArgumentException.class);
    }
}
