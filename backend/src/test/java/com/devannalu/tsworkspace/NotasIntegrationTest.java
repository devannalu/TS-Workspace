package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.autenticacao.InicializacaoIdentidadeService;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.rbac.*;
import com.devannalu.tsworkspace.notas.NotaRepository.Recurso;
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
class NotasIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_notas_test").withLabel("com.tsworkspace.purpose", "notas-test");
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
    @Autowired com.devannalu.tsworkspace.parcerias.ParceriaService parcerias;
    @Autowired com.devannalu.tsworkspace.notas.NotaService notas;
    @Autowired com.devannalu.tsworkspace.comentarios.ComentarioService comentarios;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
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
    private com.devannalu.tsworkspace.parcerias.ParceriaService.Dados dados(String equipe,String responsavel,LocalDate data,String evento,String projeto) {
        return new com.devannalu.tsworkspace.parcerias.ParceriaService.Dados("Organização parceira","Descrição segura",
            com.devannalu.tsworkspace.parcerias.ParceriaRepository.Status.PROSPECCAO,equipe,responsavel,data,"Contato principal","contato@example.test","555-0100","Enviar proposta","Informação interna",evento,projeto);
    }
    private com.devannalu.tsworkspace.parcerias.ParceriaService.Dados dados() {return dados(equipeA,suporte,LocalDate.of(2026,10,7),null,null);}
    private String evento(String equipe) {
        var id=UUID.randomUUID().toString();jdbc.update("INSERT INTO community_event(id,name,format,team_id,start_at,end_at,zone_id,created_by_id,created_at,updated_at) VALUES(?,'Evento','ONLINE',?,'2026-10-07 18:00:00','2026-10-07 19:00:00','America/Bahia',?,NOW(6),NOW(6))",id,equipe,superAdmin);return id;
    }
    private String projeto() {return projeto(equipeA,null,null,"PLANEJADO",false);}
    private String reuniao() {
        String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO meeting(id,title,type,team_id,start_at,end_at,zone_id,created_by_id,created_at,updated_at) VALUES(?,'Reunião','REUNIAO',?,'2026-10-13 15:00:00','2026-10-13 16:00:00','America/Bahia',?,NOW(6),NOW(6))",id,equipeA,superAdmin);
        return id;
    }
    @Test void documentaQuatroContextosComAutoriaETextoSimples() {
        var fontes=Map.of(Recurso.PROJETO,projeto(),Recurso.EVENTO,evento(equipeA),Recurso.PARCERIA,parcerias.criar(supervisora,dados()).id(),Recurso.REUNIAO,reuniao());
        for(var fonte:fontes.entrySet()) {
            var nota=notas.criar(supervisora,fonte.getKey(),fonte.getValue(),"  Decisão  ","<script>texto simples</script>");
            assertThat(nota.titulo()).isEqualTo("Decisão");assertThat(nota.autora().id()).isEqualTo(supervisora);
            var pagina=notas.listar(suporte,fonte.getKey(),fonte.getValue(),0,10);
            assertThat(pagina.items()).hasSize(1);assertThat(pagina.podeCriar()).isFalse();assertThat(pagina.items().get(0).editar()).isFalse();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE entity_type='Note' AND metadata_json LIKE '%script%'",Integer.class)).isZero();
    }
    @Test void apenasAutoraComCapacidadeAtualEditaOuRemove() {
        String projeto=projeto();var nota=notas.criar(supervisora,Recurso.PROJETO,projeto,"Decisão","Texto");
        assertThatThrownBy(()->notas.editar(admin,nota.id(),"Outra","Texto",0)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->notas.remover(admin,nota.id(),0)).isInstanceOf(AccessDeniedException.class);
        var alterada=notas.editar(supervisora,nota.id(),"Decisão revisada","Texto revisado",0);assertThat(alterada.versao()).isEqualTo(1);
        negar(supervisora,"projects.edit");assertThatThrownBy(()->notas.editar(supervisora,nota.id(),"Outra","Texto",1)).isInstanceOf(AccessDeniedException.class);
        assertThat(notas.listar(supervisora,Recurso.PROJETO,projeto,0,10).items().get(0).editar()).isFalse();
    }
    @Test void escopoELeituraDaFonteSaoObrigatorios() {
        String projeto=projeto();var nota=notas.criar(supervisora,Recurso.PROJETO,projeto,"Decisão","Texto");
        assertThatThrownBy(()->notas.listar(fora,Recurso.PROJETO,projeto,0,10)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->notas.criar(suporte,Recurso.PROJETO,projeto,"Decisão","Texto")).isInstanceOf(AccessDeniedException.class);
        negar(supervisora,"projects.view");assertThatThrownBy(()->notas.remover(supervisora,nota.id(),0)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void arquivoDaFonteOuDaEquipeImpedeEscrita() {
        String projeto=projeto();var nota=notas.criar(supervisora,Recurso.PROJETO,projeto,"Decisão","Texto");
        jdbc.update("UPDATE project SET archived_at=NOW(6) WHERE id=?",projeto);
        assertThat(notas.listar(supervisora,Recurso.PROJETO,projeto,0,10).podeCriar()).isFalse();
        assertThatThrownBy(()->notas.remover(supervisora,nota.id(),0)).isInstanceOf(AccessDeniedException.class);
        jdbc.update("UPDATE project SET archived_at=NULL WHERE id=?",projeto);jdbc.update("UPDATE team SET archived_at=NOW(6) WHERE id=?",equipeA);
        assertThatThrownBy(()->notas.editar(supervisora,nota.id(),"Outra","Texto",0)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void conflitoERemocaoLogicaPreservamAutoriaSemExporTextoRemovido() {
        String projeto=projeto();var nota=notas.criar(supervisora,Recurso.PROJETO,projeto,"Decisão","Texto privado");
        notas.editar(supervisora,nota.id(),"Revisão","Texto privado",0);
        assertThatThrownBy(()->notas.remover(supervisora,nota.id(),0)).isInstanceOf(ProblemaDominio.class);
        notas.remover(supervisora,nota.id(),1);assertThat(notas.listar(supervisora,Recurso.PROJETO,projeto,0,10).items()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT author_id FROM workspace_note WHERE id=? AND removed_at IS NOT NULL",String.class,nota.id())).isEqualTo(supervisora);
        assertThatThrownBy(()->notas.editar(supervisora,nota.id(),"Outra","Texto",2)).isInstanceOf(ProblemaDominio.class);
    }
    @Test void limitesPaginaConstraintEAutoriaPersistida() {
        String projeto=projeto();for(int i=0;i<3;i++)notas.criar(supervisora,Recurso.PROJETO,projeto,"Nota "+i,"Texto");
        assertThat(notas.listar(supervisora,Recurso.PROJETO,projeto,1,2).items()).hasSize(1);
        assertThatThrownBy(()->notas.listar(supervisora,Recurso.PROJETO,projeto,0,101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->notas.criar(supervisora,Recurso.PROJETO,projeto," ","Texto")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->notas.criar(supervisora,Recurso.PROJETO,projeto,"Nota","x".repeat(5001))).isInstanceOf(IllegalArgumentException.class);
        String evento=evento(equipeA);
        assertThatThrownBy(()->jdbc.update("UPDATE workspace_note SET event_id=? WHERE project_id=?",evento,projeto)).isInstanceOf(org.springframework.dao.DataAccessException.class).hasMessageContaining("note_context_ck");
    }
    @Test void http401403404409ECsrf() throws Exception {
        String projeto=projeto(),rota="/api/v1/projects/"+projeto+"/notes";var corpo=Map.of("titulo","Decisão","texto","Informação");
        http.perform(get(rota)).andExpect(status().isUnauthorized());var sessao=entrar(supervisora);
        http.perform(post(rota).cookie(sessao).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isForbidden());
        http.perform(post(rota).cookie(entrar(suporte)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isForbidden());
        var resposta=http.perform(post(rota).cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isCreated()).andReturn().getResponse();
        String id=json.readTree(resposta.getContentAsString()).get("id").asText();
        http.perform(patch("/api/v1/notes/"+id).cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("titulo","Decisão","texto","Informação","versao",99)))).andExpect(status().isConflict());
        http.perform(patch("/api/v1/notes/"+UUID.randomUUID()).cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("titulo","Decisão","texto","Informação","versao",0)))).andExpect(status().isNotFound());
    }
    @Test void concorrenciaMantemUmaVersaoVencedora() throws Exception {
        var nota=notas.criar(supervisora,Recurso.PROJETO,projeto(),"Decisão","Texto");
        var inicio=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {var resultados=new ArrayList<java.util.concurrent.Future<Boolean>>();for(int i=0;i<2;i++)resultados.add(pool.submit(()->{inicio.await();try{notas.editar(supervisora,nota.id(),"Decisão","Revisão",0);return true;}catch(ProblemaDominio e){assertThat(e.status()).isEqualTo(409);return false;}}));
            inicio.countDown();int sucesso=0;for(var resultado:resultados)if(resultado.get(15,java.util.concurrent.TimeUnit.SECONDS))sucesso++;assertThat(sucesso).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void atividadeDoProjetoConsolidaNotasSemExporCorpoRemovido() throws Exception {
        String projeto=projeto(),outro=projeto();var nota=notas.criar(supervisora,Recurso.PROJETO,projeto,"Decisão","Corpo privado");
        notas.editar(supervisora,nota.id(),"Decisão revisada","Corpo privado revisado",0);notas.remover(supervisora,nota.id(),1);
        notas.criar(supervisora,Recurso.PROJETO,outro,"Outra nota","Outro corpo privado");
        var tipo=com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso.PROJETO;
        var atividade=comentarios.listarAtividade(suporte,tipo,projeto,0,25);
        assertThat(atividade.items()).extracting(e->e.tipo()).containsExactlyInAnyOrder("note.created","note.updated","note.removed");
        assertThat(atividade.total()).isEqualTo(3);assertThat(json.writeValueAsString(atividade)).doesNotContain("Corpo privado","Decisão","Outro corpo");
        assertThatThrownBy(()->comentarios.listarAtividade(fora,tipo,projeto,0,25)).isInstanceOf(AccessDeniedException.class);
    }

}
