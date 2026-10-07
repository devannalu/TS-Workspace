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
class ReunioesIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_reunioes_test").withLabel("com.tsworkspace.purpose", "reunioes-test");
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
    @Autowired com.devannalu.tsworkspace.reunioes.ReuniaoService reunioes;
    @Autowired com.devannalu.tsworkspace.calendario.CalendarioService calendario;
    @Autowired com.devannalu.tsworkspace.tarefas.TarefaService tarefas;
    @Autowired com.devannalu.tsworkspace.projetos.ProjetoService projetos;
    @Autowired com.devannalu.tsworkspace.comentarios.ComentarioService comentarios;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM meeting"); jdbc.update("DELETE FROM attachment"); jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM workspace_comment"); jdbc.update("DELETE FROM task"); jdbc.update("DELETE FROM project");
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
    private com.devannalu.tsworkspace.reunioes.ReuniaoService.Dados dados(String zona) {
        return new com.devannalu.tsworkspace.reunioes.ReuniaoService.Dados("Encontro de planejamento","Pauta do encontro",
            com.devannalu.tsworkspace.reunioes.ReuniaoRepository.Tipo.REUNIAO,com.devannalu.tsworkspace.reunioes.ReuniaoRepository.Status.AGENDADA,
            equipeA,LocalDateTime.of(2026,10,7,15,0),LocalDateTime.of(2026,10,7,16,0),zona,"Sala virtual","https://example.test/encontro",null,List.of(suporte),List.of(supervisora));
    }
    @Test void fusosPreservamHorarioLocalEInstantCorreto() {
        var bahia=reunioes.criar(supervisora,dados("America/Bahia"));var lisboa=reunioes.criar(supervisora,dados("Europe/Lisbon"));
        assertThat(bahia.inicio()).isEqualTo(Instant.parse("2026-10-07T18:00:00Z"));
        assertThat(lisboa.inicio()).isEqualTo(Instant.parse("2026-10-07T14:00:00Z"));
        assertThat(bahia.inicioLocal()).isEqualTo(lisboa.inicioLocal());assertThat(lisboa.zona()).isEqualTo("Europe/Lisbon");
        assertThat(lisboa.participantes()).hasSize(2);assertThat(lisboa.participantes().stream().filter(p->p.responsavel()).count()).isEqualTo(1);
    }
    @Test void transicoesDeVeraoNaoEscolhemHorarioSilenciosamente() {
        assertThatThrownBy(()->com.devannalu.tsworkspace.reunioes.ReuniaoService.resolverHorario(LocalDateTime.of(2026,3,29,1,30),"Europe/Lisbon")).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->com.devannalu.tsworkspace.reunioes.ReuniaoService.resolverHorario(LocalDateTime.of(2026,10,25,1,30),"Europe/Lisbon")).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(()->com.devannalu.tsworkspace.reunioes.ReuniaoService.resolverHorario(LocalDateTime.now(),"Fuso/Invalido")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void edicaoVersaoArquivoEHistorico() {
        var encontro=reunioes.criar(supervisora,dados("America/Bahia"));var atual=reunioes.editar(supervisora,encontro.id(),dados("Europe/Lisbon"),0);
        assertThat(atual.versao()).isEqualTo(1);assertThat(atual.zona()).isEqualTo("Europe/Lisbon");
        assertThatThrownBy(()->reunioes.editar(supervisora,encontro.id(),dados("America/Bahia"),0)).isInstanceOf(ProblemaDominio.class);
        var arquivo=reunioes.arquivar(superAdmin,encontro.id(),atual.versao());assertThat(arquivo.arquivada()).isTrue();assertThat(arquivo.capacidades().editar()).isFalse();
        assertThat(reunioes.listar(supervisora,null,null,null,false,0,25).total()).isZero();
        assertThat(reunioes.listar(supervisora,null,null,null,true,0,25).total()).isEqualTo(1);
        assertThatThrownBy(()->reunioes.editar(superAdmin,encontro.id(),dados("America/Bahia"),arquivo.versao())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void escopoPermissionESuporteSomenteLeitura() {
        var encontro=reunioes.criar(supervisora,dados("America/Bahia"));
        assertThat(reunioes.detalhe(suporte,encontro.id()).capacidades().editar()).isFalse();
        assertThat(reunioes.listar(fora,null,null,null,false,0,25).items()).isEmpty();
        assertThatThrownBy(()->reunioes.detalhe(fora,encontro.id())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->reunioes.criar(suporte,dados("America/Bahia"))).isInstanceOf(AccessDeniedException.class);
        negar(supervisora,"meetings.view");assertThatThrownBy(()->reunioes.detalhe(supervisora,encontro.id())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void participanteExternoFalhaSemPersistenciaParcial() {
        var d=dados("America/Bahia");var invalido=new com.devannalu.tsworkspace.reunioes.ReuniaoService.Dados(d.titulo(),d.pauta(),d.tipo(),d.status(),d.equipeId(),d.inicioLocal(),d.fimLocal(),d.zona(),d.local(),d.link(),d.resultados(),List.of(fora),List.of(supervisora));
        assertThatThrownBy(()->reunioes.criar(supervisora,invalido)).isInstanceOf(ProblemaDominio.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM meeting",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE entity_type='Meeting'",Integer.class)).isZero();
    }
    @Test void calendarioMistoMantemDateOnlyEIncluiHorarioAutorizado() {
        var encontro=reunioes.criar(supervisora,dados("Europe/Lisbon"));String tarefa=tarefa(equipeA,LocalDate.of(2026,10,7),"A_FAZER",false);
        var itens=calendario.listar(supervisora,LocalDate.of(2026,10,7),LocalDate.of(2026,10,7),null,null,null);
        assertThat(itens).hasSize(2);var reuniao=itens.stream().filter(i->i.recursoId().equals(encontro.id())).findFirst().orElseThrow();
        assertThat(reuniao.inicioEm()).isEqualTo(encontro.inicio());assertThat(reuniao.zona()).isEqualTo("Europe/Lisbon");
        var prazo=itens.stream().filter(i->i.recursoId().equals(tarefa)).findFirst().orElseThrow();assertThat(prazo.dataInicio()).isEqualTo(LocalDate.of(2026,10,7));assertThat(prazo.inicioEm()).isNull();
        negar(suporte,"tasks.view");negar(suporte,"projects.view");
        assertThat(calendario.listar(suporte,LocalDate.of(2026,10,7),LocalDate.of(2026,10,7),null,null,null)).extracting(i->i.tipo().name()).containsExactly("REUNIAO");
        negar(supervisora,"meetings.view");assertThat(calendario.listar(supervisora,LocalDate.of(2026,10,7),LocalDate.of(2026,10,7),null,null,null)).hasSize(1);
        assertThat(calendario.listar(fora,LocalDate.of(2026,10,7),LocalDate.of(2026,10,7),null,null,null)).isEmpty();
    }
    @Test void filtrosPaginacaoECatalogoSeedIdempotente() {
        for(int i=0;i<3;i++)reunioes.criar(supervisora,dados("America/Bahia"));
        assertThat(reunioes.listar(supervisora,equipeA,com.devannalu.tsworkspace.reunioes.ReuniaoRepository.Status.AGENDADA,"planejamento",false,1,2).items()).hasSize(1);
        assertThat(reunioes.listar(supervisora,null,null,"%",false,0,25).items()).isEmpty();
        assertThatThrownBy(()->reunioes.listar(supervisora,null,null,null,false,0,101)).isInstanceOf(IllegalArgumentException.class);
        seed.seed();seed.seed();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permissions WHERE permission_key LIKE 'meetings.%'",Integer.class)).isEqualTo(4);
    }
    @Test void concorrenciaMantemUmaEdicaoVencedora() throws Exception {
        var encontro=reunioes.criar(supervisora,dados("America/Bahia"));var inicio=new java.util.concurrent.CountDownLatch(1);var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {var respostas=new ArrayList<java.util.concurrent.Future<Boolean>>();for(int i=0;i<2;i++)respostas.add(pool.submit(()->{inicio.await();try{reunioes.editar(supervisora,encontro.id(),dados("Europe/Lisbon"),0);return true;}catch(ProblemaDominio e){assertThat(e.status()).isEqualTo(409);return false;}}));
            inicio.countDown();int sucessos=0;for(var r:respostas)if(r.get(15,java.util.concurrent.TimeUnit.SECONDS))sucessos++;assertThat(sucessos).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    @Test void httpExigeSessaoCsrfERejeitaUrlInsegura() throws Exception {
        http.perform(get("/api/v1/meetings")).andExpect(status().isUnauthorized());var cookie=entrar(supervisora);
        var d=dados("America/Bahia");var corpo=new HashMap<String,Object>();corpo.put("titulo",d.titulo());corpo.put("tipo","REUNIAO");corpo.put("status","AGENDADA");corpo.put("equipeId",equipeA);corpo.put("inicioLocal","2026-10-07T15:00:00");corpo.put("fimLocal","2026-10-07T16:00:00");corpo.put("zona","America/Bahia");
        http.perform(post("/api/v1/meetings").cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isForbidden());
        corpo.put("link","javascript:alert(1)");http.perform(post("/api/v1/meetings").cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isBadRequest());
        corpo.put("link","https://example.test");http.perform(post("/api/v1/meetings").cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo))).andExpect(status().isCreated()).andExpect(jsonPath("$.inicio").value("2026-10-07T18:00:00Z"));
        http.perform(get("/api/v1/meetings").cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }
}
