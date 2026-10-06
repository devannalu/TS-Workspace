package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.autenticacao.InicializacaoIdentidadeService;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.equipes.EquipeService;
import com.devannalu.tsworkspace.rbac.*;
import com.devannalu.tsworkspace.tarefas.*;
import com.devannalu.tsworkspace.usuarios.UsuarioService;
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
class TarefasIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_tarefas_test").withLabel("com.tsworkspace.purpose", "tarefas-test");
    @DynamicPropertySource static void banco(DynamicPropertyRegistry configuracao) {
        configuracao.add("spring.datasource.url", MYSQL::getJdbcUrl);
        configuracao.add("spring.datasource.username", MYSQL::getUsername);
        configuracao.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired TarefaService tarefas;
    @Autowired EquipeService equipes;
    @Autowired UsuarioService usuarios;
    @Autowired InicializacaoIdentidadeService identidade;
    @Autowired RbacSeed seed;
    @Autowired PermissaoService permissoes;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired MockMvc http;
    @Autowired Flyway flyway;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM task");
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
    private TarefaService.TarefaResponse criar(String autora, String equipe, String titulo, List<String> responsaveis) {
        return tarefas.criarTarefa(autora, titulo, "Descrição pesquisável", null, equipe, null, responsaveis);
    }
    private TarefaService.FiltrosTarefas filtro(String equipe, Tarefa.Status status, Tarefa.Prioridade prioridade,
        String responsavel, String busca, LocalDate de, LocalDate ate, boolean arquivadas) {
        return new TarefaService.FiltrosTarefas(equipe, status, prioridade, responsavel, busca, de, ate, arquivadas, 0, 25);
    }
    private TarefaService.TarefaResponse editar(String autora, TarefaService.TarefaResponse tarefa, String equipe, List<String> responsaveis) {
        return tarefas.editarTarefa(autora, tarefa.id(), "Título editado", "Descrição editada", Tarefa.Prioridade.ALTA,
            equipe, LocalDate.now().plusDays(2), responsaveis, tarefa.versao());
    }
    private Cookie entrar(String usuario) throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM app_user WHERE id=?", String.class, usuario);
        return http.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", SENHA_TESTE))))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("TS_SESSION");
    }

    @Test void deveAplicarV8ComConstraintsSemReexecutarMigracoes() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("9");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('task','task_assignee')", Long.class)).isEqualTo(2);
    }
    @Test void devePreservarSeedIdempotenteEMatrizNova() {
        seed.seed(); seed.seed();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class)).isEqualTo(24);
        for (String pessoa : List.of(superAdmin, admin, supervisora, suporte)) {
            assertThat(permissoes.possuiPermissao(pessoa, "tasks.view")).isTrue();
            assertThat(permissoes.possuiPermissao(pessoa, "tasks.create")).isTrue();
            assertThat(permissoes.possuiPermissao(pessoa, "tasks.edit")).isTrue();
        }
        assertThat(permissoes.possuiPermissao(supervisora, "tasks.assign")).isTrue();
        assertThat(permissoes.possuiPermissao(suporte, "tasks.assign")).isFalse();
        assertThat(permissoes.possuiPermissao(supervisora, "tasks.archive")).isFalse();
        assertThat(permissoes.possuiPermissao(suporte, "tasks.archive")).isFalse();
    }
    @Test void deveCriarComTrimStatusInicialEPrioridadePadrao() {
        var tarefa = criar(suporte, equipeA, "  Planejar encontro  ", List.of());
        assertThat(tarefa.titulo()).isEqualTo("Planejar encontro");
        assertThat(tarefa.prioridade()).isEqualTo(Tarefa.Prioridade.MEDIA);
        assertThat(tarefa.status()).isEqualTo(Tarefa.Status.A_FAZER);
        assertThat(tarefa.criadaPor().id()).isEqualTo(suporte);
    }
    @Test void deveExigirEquipeTituloETextoLimitado() {
        assertThatThrownBy(() -> criar(admin, null, "Título", List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> criar(admin, equipeA, " ", List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tarefas.criarTarefa(admin, "Título", "x".repeat(5001), null, equipeA, null, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deveRetornarDetalheComMultiplasResponsaveisSemEntidadesOuSegredos() throws Exception {
        var tarefa = criar(admin, equipeA, "Encontro", List.of(suporte, outra));
        assertThat(tarefas.buscarTarefa(suporte, tarefa.id()).responsaveis()).hasSize(2);
        assertThat(json.writeValueAsString(tarefa)).doesNotContain("password", "email", "session");
    }
    @Test void deveRejeitarResponsavelForaDaEquipeOuInativa() {
        assertThatThrownBy(() -> criar(admin, equipeA, "Encontro", List.of(fora))).isInstanceOf(IllegalArgumentException.class);
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?", suporte);
        assertThatThrownBy(() -> criar(admin, equipeA, "Encontro", List.of(suporte))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void deveEditarConteudoPrazoEPrioridade() {
        var tarefa = criar(admin, equipeA, "Encontro", List.of(suporte));
        var editada = editar(admin, tarefa, equipeA, List.of(suporte, outra));
        assertThat(editada.titulo()).isEqualTo("Título editado"); assertThat(editada.prioridade()).isEqualTo(Tarefa.Prioridade.ALTA);
        assertThat(editada.prazo()).isEqualTo(LocalDate.now().plusDays(2)); assertThat(editada.responsaveis()).hasSize(2);
    }
    @Test void deveRevalidarResponsaveisAoTrocarEquipe() {
        var tarefa = criar(admin, equipeA, "Encontro", List.of(suporte));
        assertThatThrownBy(() -> editar(admin, tarefa, equipeB, null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(editar(admin, tarefa, equipeB, List.of(fora)).equipe().id()).isEqualTo(equipeB);
    }
    @Test void deveImpedirRemocaoDeIntegranteComResponsabilidadeAtiva() {
        var tarefa = criar(admin, equipeA, "Encontro", List.of(suporte));
        assertThatThrownBy(() -> equipes.removerIntegrante(equipeA, suporte)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(() -> usuarios.editarUsuario(superAdmin, suporte, null, null, List.of())).isInstanceOf(ProblemaDominio.class);
        tarefas.arquivarTarefa(admin, tarefa.id(), tarefa.versao());
        assertThatCode(() -> equipes.removerIntegrante(equipeA, suporte)).doesNotThrowAnyException();
    }
    @Test void devePermitirSuporteEditarSomenteCriadasOuAtribuidas() {
        var propria = criar(suporte, equipeA, "Própria", List.of());
        assertThatCode(() -> editar(suporte, propria, equipeA, null)).doesNotThrowAnyException();
        var atribuida = criar(admin, equipeA, "Atribuída", List.of(suporte));
        assertThatCode(() -> editar(suporte, atribuida, equipeA, null)).doesNotThrowAnyException();
        var alheia = criar(outra, equipeA, "Alheia", List.of());
        assertThat(tarefas.buscarTarefa(suporte, alheia.id()).capacidades().editar()).isFalse();
        assertThatThrownBy(() -> editar(suporte, alheia, equipeA, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tarefas.moverTarefa(suporte, alheia.id(), Tarefa.Status.CONCLUIDA, null, alheia.versao())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void deveImpedirSuporteDeAtribuirMesmoSuaPropriaTarefa() {
        var tarefa = criar(suporte, equipeA, "Própria", List.of());
        assertThatThrownBy(() -> editar(suporte, tarefa, equipeA, List.of(suporte))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> criar(suporte, equipeA, "Outra", List.of(suporte))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void deveLimitarSupervisoraEListagemAsPropriasEquipes() {
        var local = criar(admin, equipeA, "Local", List.of()); var externa = criar(admin, equipeB, "Externa", List.of());
        assertThat(tarefas.listarTarefas(supervisora, filtro(null,null,null,null,null,null,null,false)).items()).extracting(TarefaService.TarefaResponse::id).containsExactly(local.id());
        assertThatCode(() -> editar(supervisora, local, equipeA, List.of(suporte))).doesNotThrowAnyException();
        assertThatThrownBy(() -> tarefas.buscarTarefa(supervisora, externa.id())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> criar(supervisora, equipeB, "Externa", List.of())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tarefas.buscarOpcoes(supervisora, equipeB)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void devePermitirAdministracaoGlobalSemMembership() {
        var tarefa = criar(admin, equipeB, "Global", List.of(fora));
        assertThat(editar(admin, tarefa, equipeB, List.of(fora)).titulo()).isEqualTo("Título editado");
        assertThat(tarefas.buscarOpcoes(admin, null).equipes()).hasSize(5);
    }
    @Test void deveFiltrarEquipeResponsavelPrioridadeStatusEBusca() {
        var tarefa = tarefas.criarTarefa(admin, "Planejar encontro", "Descrição pesquisável", Tarefa.Prioridade.ALTA, equipeA, null, List.of(suporte));
        criar(admin, equipeB, "Outra", List.of());
        assertThat(tarefas.listarTarefas(admin, filtro(equipeA,Tarefa.Status.A_FAZER,Tarefa.Prioridade.ALTA,suporte,"pesquisável",null,null,false)).items())
            .extracting(TarefaService.TarefaResponse::id).containsExactly(tarefa.id());
        assertThat(tarefas.listarTarefas(admin, filtro(null,null,null,null,"%",null,null,false)).total()).isZero();
    }
    @Test void deveFiltrarIntervaloDePrazoEDerivarAtraso() {
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Bahia"));
        var tarefa = tarefas.criarTarefa(admin,"Vencida",null,null,equipeA,hoje.minusDays(1),List.of(suporte));
        assertThat(tarefa.atrasada()).isTrue();
        assertThat(tarefas.listarTarefas(admin,filtro(null,null,null,null,null,hoje.minusDays(2),hoje,false)).total()).isEqualTo(1);
        assertThat(tarefas.moverTarefa(admin,tarefa.id(),Tarefa.Status.CONCLUIDA,null,tarefa.versao()).atrasada()).isFalse();
    }
    @Test void deveMoverEReordenarColunasComOrdemContigua() {
        var a=criar(admin,equipeA,"Primeira",List.of()); var b=criar(admin,equipeA,"Segunda",List.of()); var c=criar(admin,equipeA,"Terceira",List.of());
        tarefas.moverTarefa(admin,c.id(),Tarefa.Status.A_FAZER,a.id(),c.versao());
        var lista=tarefas.listarTarefas(admin,filtro(null,Tarefa.Status.A_FAZER,null,null,null,null,null,false)).items();
        assertThat(lista).extracting(TarefaService.TarefaResponse::id).containsExactly(c.id(),a.id(),b.id());
        assertThat(lista).extracting(TarefaService.TarefaResponse::ordem).containsExactly(0,1,2);
        var atual=tarefas.buscarTarefa(admin,a.id());
        tarefas.moverTarefa(admin,a.id(),Tarefa.Status.EM_REVISAO,null,atual.versao());
        assertThat(tarefas.listarTarefas(admin,filtro(null,Tarefa.Status.A_FAZER,null,null,null,null,null,false)).items()).extracting(TarefaService.TarefaResponse::ordem).containsExactly(0,1);
    }
    @Test void deveAuditarStatusMasNaoCadaReordenacao() {
        var a=criar(admin,equipeA,"Primeira",List.of(suporte)); var b=criar(admin,equipeA,"Segunda",List.of());
        var atual=tarefas.moverTarefa(admin,a.id(),Tarefa.Status.A_FAZER,null,a.versao());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='task.status_changed'",Integer.class)).isZero();
        tarefas.moverTarefa(admin,a.id(),Tarefa.Status.EM_ANDAMENTO,null,atual.versao());
        var editada=editar(admin,tarefas.buscarTarefa(admin,a.id()),equipeA,List.of(outra));
        tarefas.arquivarTarefa(admin,a.id(),editada.versao());
        assertThat(jdbc.queryForList("SELECT DISTINCT action FROM audit_log",String.class)).contains("task.created","task.updated","task.status_changed","task.assignees_changed","task.archived");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE metadata_json IS NOT NULL",Long.class)).isZero();
        assertThat(b.id()).isNotBlank();
    }
    @Test void deveArquivarSemApagarHistoricoEExcluirDoBoard() {
        var tarefa=criar(admin,equipeA,"Histórica",List.of(suporte));
        var arquivada=tarefas.arquivarTarefa(admin,tarefa.id(),tarefa.versao());
        assertThat(arquivada.arquivada()).isTrue();
        assertThat(tarefas.listarTarefas(admin,filtro(null,null,null,null,null,null,null,false)).total()).isZero();
        assertThat(tarefas.listarTarefas(admin,filtro(null,null,null,null,null,null,null,true)).total()).isEqualTo(1);
        assertThatThrownBy(() -> editar(admin,arquivada,equipeA,null)).isInstanceOf(ProblemaDominio.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM task_assignee",Long.class)).isEqualTo(1);
    }
    @Test void deveNegarArquivamentoASupervisoraESuporte() {
        var tarefa=criar(suporte,equipeA,"Própria",List.of());
        for(String pessoa:List.of(supervisora,suporte))assertThatThrownBy(() -> tarefas.arquivarTarefa(pessoa,tarefa.id(),tarefa.versao())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void deveManterHistoricoDeEquipeArquivadaSomenteLeitura() {
        var tarefa=criar(admin,equipeA,"Histórica",List.of(suporte));
        jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP(6) WHERE id=?",equipeA);
        assertThat(tarefas.buscarTarefa(suporte,tarefa.id()).capacidades().editar()).isFalse();
        assertThatThrownBy(() -> editar(admin,tarefa,equipeA,null)).isInstanceOf(ProblemaDominio.class);
        assertThatThrownBy(() -> criar(admin,equipeA,"Nova",List.of())).isInstanceOf(ProblemaDominio.class);
        assertThat(tarefas.arquivarTarefa(admin,tarefa.id(),tarefa.versao()).arquivada()).isTrue();
    }
    @Test void deveResumirSomenteAtribuidasDentroDoEscopo() {
        LocalDate hoje=LocalDate.now(ZoneId.of("America/Bahia"));
        var vencida=tarefas.criarTarefa(admin,"Atrasada",null,null,equipeA,hoje.minusDays(1),List.of(suporte));
        tarefas.moverTarefa(admin,vencida.id(),Tarefa.Status.EM_ANDAMENTO,null,vencida.versao());
        tarefas.criarTarefa(admin,"Hoje",null,null,equipeA,hoje,List.of(suporte));
        criar(admin,equipeA,"Sem atribuição",List.of());
        assertThat(tarefas.resumirTarefas(suporte)).isEqualTo(new TarefaService.ResumoTarefas(2,1,1,1));
        jdbc.update("DELETE FROM team_member WHERE user_id=? AND team_id=?",suporte,equipeA);
        assertThat(tarefas.resumirTarefas(suporte).minhasTarefas()).isZero();
    }
    @Test void deveRejeitarDuasEdicoesConcorrentesDaMesmaVersao() throws Exception {
        var tarefa=criar(admin,equipeA,"Concorrente",List.of());
        var prontas=new CountDownLatch(2); var iniciar=new CountDownLatch(1); var executor=Executors.newFixedThreadPool(2);
        Callable<Boolean> tentativa=()->{prontas.countDown();iniciar.await(10,TimeUnit.SECONDS);try{editar(admin,tarefa,equipeA,List.of());return true;}catch(ProblemaDominio erro){assertThat(erro.status()).isEqualTo(409);return false;}};
        try {
            var primeira=executor.submit(tentativa);var segunda=executor.submit(tentativa);
            assertThat(prontas.await(10,TimeUnit.SECONDS)).isTrue();iniciar.countDown();
            assertThat(List.of(primeira.get(20,TimeUnit.SECONDS),segunda.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(tarefas.buscarTarefa(admin,tarefa.id()).versao()).isEqualTo(1);
        } finally {iniciar.countDown();executor.shutdownNow();}
    }
    @Test void deveRetornar401403404E409SemDetalhesTecnicos() throws Exception {
        http.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
        var sessao=entrar(suporte);var tarefa=criar(suporte,equipeA,"Própria",List.of());
        http.perform(post("/api/v1/tasks").cookie(sessao).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        http.perform(get("/api/v1/tasks/"+UUID.randomUUID()).cookie(sessao)).andExpect(status().isNotFound());
        http.perform(get("/api/v1/tasks/"+criar(admin,equipeB,"Externa",List.of()).id()).cookie(sessao)).andExpect(status().isForbidden());
        editar(suporte,tarefa,equipeA,null);
        http.perform(patch("/api/v1/tasks/"+tarefa.id()+"/position").cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("status","EM_ANDAMENTO","versao",tarefa.versao())))).andExpect(status().isConflict());
        http.perform(delete("/api/v1/tasks/"+tarefa.id()).cookie(sessao).with(csrf())).andExpect(status().isForbidden());
    }
    @Test void deveCriarPorHttpSemAceitarCriadoraDoCliente() throws Exception {
        var dados=new HashMap<String,Object>();dados.put("titulo","HTTP");dados.put("equipeId",equipeA);dados.put("criadaPorId",fora);
        http.perform(post("/api/v1/tasks").cookie(entrar(suporte)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.criadaPor.id").value(suporte)).andExpect(jsonPath("$.status").value("A_FAZER"));
    }
    @Test void deveValidarFiltrosEPayloadMalformados() throws Exception {
        var sessao=entrar(admin);
        for(String filtro:List.of("size=101","page=-1","status=BLOQUEADA","teamId=invalido","dueFrom=2026-10-20&dueTo=2026-10-01"))
            http.perform(get("/api/v1/tasks?"+filtro).cookie(sessao)).andExpect(status().isBadRequest());
        http.perform(post("/api/v1/tasks").cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }
    @Test void deveNegarOverrideDePermissionSemLiberarEscopo() {
        jdbc.update("INSERT INTO user_permissions (user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key='tasks.edit'",suporte);
        var tarefa=criar(suporte,equipeA,"Própria",List.of());
        assertThatThrownBy(() -> editar(suporte,tarefa,equipeA,null)).isInstanceOf(AccessDeniedException.class);
        assertThat(tarefas.buscarTarefa(suporte,tarefa.id()).capacidades().editar()).isFalse();
    }
}
