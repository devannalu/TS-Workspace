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
class ParceriasIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_parcerias_test").withLabel("com.tsworkspace.purpose", "parcerias-test");
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
    @Autowired com.devannalu.tsworkspace.calendario.CalendarioService calendario;
    @Autowired com.devannalu.tsworkspace.tarefas.TarefaService tarefas;
    @Autowired com.devannalu.tsworkspace.projetos.ProjetoService projetos;
    @Autowired com.devannalu.tsworkspace.comentarios.ComentarioService comentarios;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM partnership"); jdbc.update("DELETE FROM communication_content"); jdbc.update("DELETE FROM community_event"); jdbc.update("DELETE FROM meeting"); jdbc.update("DELETE FROM attachment"); jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM workspace_comment"); jdbc.update("DELETE FROM task"); jdbc.update("DELETE FROM project");
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
    private com.devannalu.tsworkspace.parcerias.ParceriaService.Dados dados(String equipe,String responsavel,LocalDate data,String evento,String projeto) {
        return new com.devannalu.tsworkspace.parcerias.ParceriaService.Dados("Organização parceira","Descrição segura",
            com.devannalu.tsworkspace.parcerias.ParceriaRepository.Status.PROSPECCAO,equipe,responsavel,data,"Contato principal","contato@example.test","555-0100","Enviar proposta","Informação interna",evento,projeto);
    }
    private com.devannalu.tsworkspace.parcerias.ParceriaService.Dados dados() {return dados(equipeA,suporte,LocalDate.of(2026,10,7),null,null);}
    private String evento(String equipe) {
        var id=UUID.randomUUID().toString();jdbc.update("INSERT INTO community_event(id,name,format,team_id,start_at,end_at,zone_id,created_by_id,created_at,updated_at) VALUES(?,'Evento','ONLINE',?,'2026-10-07 18:00:00','2026-10-07 19:00:00','America/Bahia',?,NOW(6),NOW(6))",id,equipe,superAdmin);return id;
    }
    @Test void criaEAtualizaFluxoSemConverterDateOnly() {
        var d=dados();var c=parcerias.criar(supervisora,d);assertThat(c.dataProximaAcao()).isEqualTo(LocalDate.of(2026,10,7));
        for(var status:com.devannalu.tsworkspace.parcerias.ParceriaRepository.Status.values()) {
            c=parcerias.editar(supervisora,c.id(),new com.devannalu.tsworkspace.parcerias.ParceriaService.Dados(d.organizacao(),d.descricao(),status,d.equipeId(),d.responsavelId(),d.dataProximaAcao(),d.contatoPrincipal(),d.email(),d.telefone(),d.proximaAcao(),d.observacoes(),null,null),c.versao());assertThat(c.status()).isEqualTo(status);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE entity_type='Partnership'",Integer.class)).isEqualTo(6);
    }
    @Test void versaoArquivoEHistorico() {
        var c=parcerias.criar(supervisora,dados());var e=parcerias.editar(supervisora,c.id(),dados(),0);
        assertThatThrownBy(()->parcerias.editar(supervisora,c.id(),dados(),0)).isInstanceOf(ProblemaDominio.class);
        var a=parcerias.arquivar(superAdmin,c.id(),e.versao());assertThat(a.capacidades().editar()).isFalse();
        assertThat(parcerias.listar(supervisora,null,null,null,false,0,25).total()).isZero();assertThat(parcerias.listar(supervisora,null,null,null,true,0,25).total()).isEqualTo(1);
        assertThatThrownBy(()->parcerias.editar(superAdmin,c.id(),dados(),a.versao())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void escopoRbacESuporteSomenteLeitura() {
        var c=parcerias.criar(supervisora,dados());assertThat(parcerias.detalhe(suporte,c.id()).capacidades().editar()).isFalse();
        assertThat(parcerias.listar(fora,null,null,null,false,0,25).items()).isEmpty();
        assertThatThrownBy(()->parcerias.detalhe(fora,c.id())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->parcerias.criar(suporte,dados())).isInstanceOf(AccessDeniedException.class);
        negar(supervisora,"partnerships.view");assertThatThrownBy(()->parcerias.detalhe(supervisora,c.id())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void responsavelExternaOuInativaNaoCriaDadosParciais() {
        assertThatThrownBy(()->parcerias.criar(supervisora,dados(equipeA,fora,null,null,null))).isInstanceOf(ProblemaDominio.class);
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?",suporte);
        assertThatThrownBy(()->parcerias.criar(supervisora,dados())).isInstanceOf(ProblemaDominio.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM partnership",Integer.class)).isZero();
    }
    @Test void vinculosExigemMesmaEquipePermissaoEMantemHistorico() {
        var projeto=projeto(equipeA,null,null,"PLANEJADO",false);var evento=evento(equipeA);
        var d=dados(equipeA,suporte,null,evento,projeto);var c=parcerias.criar(supervisora,d);assertThat(c.eventoId()).isEqualTo(evento);assertThat(c.projetoId()).isEqualTo(projeto);
        jdbc.update("UPDATE project SET archived_at=NOW(6) WHERE id=?",projeto);jdbc.update("UPDATE community_event SET archived_at=NOW(6) WHERE id=?",evento);
        negar(supervisora,"events.view");negar(supervisora,"projects.view");
        assertThat(parcerias.editar(supervisora,c.id(),d,0).capacidades().verEvento()).isFalse();
        assertThatThrownBy(()->parcerias.criar(supervisora,d)).isInstanceOf(AccessDeniedException.class);
        var externo=evento(equipeB);assertThatThrownBy(()->parcerias.criar(superAdmin,dados(equipeA,null,null,externo,null))).isInstanceOf(ProblemaDominio.class);
        assertThat(parcerias.opcoes(suporte,equipeA).projetos()).isEmpty();
    }
    @Test void filtrosLimitesEBuscaLiteral() {
        for(int i=0;i<3;i++)parcerias.criar(supervisora,dados());
        assertThat(parcerias.listar(supervisora,equipeA,com.devannalu.tsworkspace.parcerias.ParceriaRepository.Status.PROSPECCAO,"parceira",false,1,2).items()).hasSize(1);
        assertThat(parcerias.listar(supervisora,null,null,"%",false,0,25).total()).isZero();
        assertThatThrownBy(()->parcerias.listar(supervisora,null,null,null,false,0,101)).isInstanceOf(IllegalArgumentException.class);
        seed.seed();seed.seed();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permissions WHERE permission_key LIKE 'partnerships.%'",Integer.class)).isEqualTo(4);
    }
    @Test void contatoNaoVazaNaAuditoriaEEmailInvalidoNaoPersiste() {
        var d=dados();var c=parcerias.criar(supervisora,d);
        assertThat(c.email()).isEqualTo("contato@example.test");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE entity_id=? AND metadata_json LIKE '%contato@example.test%'",Integer.class,c.id())).isZero();
        var invalido=new com.devannalu.tsworkspace.parcerias.ParceriaService.Dados(d.organizacao(),d.descricao(),d.status(),d.equipeId(),d.responsavelId(),d.dataProximaAcao(),d.contatoPrincipal(),"sem-arroba",d.telefone(),d.proximaAcao(),d.observacoes(),null,null);
        assertThatThrownBy(()->parcerias.criar(supervisora,invalido)).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM partnership",Integer.class)).isEqualTo(1);
    }
    @Test void http401403404409ECsrf() throws Exception {
        http.perform(get("/api/v1/partnerships")).andExpect(status().isUnauthorized());var sessao=entrar(supervisora);
        var d=dados();var corpo=new HashMap<String,Object>();corpo.put("organizacao",d.organizacao());corpo.put("status","PROSPECCAO");corpo.put("equipeId",equipeA);corpo.put("dataProximaAcao","2026-10-07");
        http.perform(post("/api/v1/partnerships").cookie(sessao).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isForbidden());
        var c=parcerias.criar(supervisora,d);corpo.put("versao",99);
        http.perform(patch("/api/v1/partnerships/"+c.id()).cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isConflict());
        http.perform(get("/api/v1/partnerships/"+UUID.randomUUID()).cookie(sessao)).andExpect(status().isNotFound());
        http.perform(post("/api/v1/partnerships").cookie(entrar(suporte)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isForbidden());
        http.perform(post("/api/v1/partnerships").cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isCreated()).andExpect(jsonPath("$.dataProximaAcao").value("2026-10-07"));
    }
    @Test void concorrenciaMantemUmaEdicaoVencedora() throws Exception {
        var c=parcerias.criar(supervisora,dados());var inicio=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {var resultados=new ArrayList<java.util.concurrent.Future<Boolean>>();for(int i=0;i<2;i++)resultados.add(pool.submit(()->{inicio.await();try{parcerias.editar(supervisora,c.id(),dados(),0);return true;}catch(ProblemaDominio e){assertThat(e.status()).isEqualTo(409);return false;}}));
            inicio.countDown();int sucesso=0;for(var r:resultados)if(r.get(15,java.util.concurrent.TimeUnit.SECONDS))sucesso++;assertThat(sucesso).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
}
