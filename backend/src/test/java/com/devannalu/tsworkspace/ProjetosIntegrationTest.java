package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.autenticacao.InicializacaoIdentidadeService;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.equipes.EquipeService;
import com.devannalu.tsworkspace.rbac.*;
import com.devannalu.tsworkspace.tarefas.*;
import com.devannalu.tsworkspace.projetos.*;
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
class ProjetosIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_projetos_test").withLabel("com.tsworkspace.purpose", "projetos-test");
    @DynamicPropertySource static void banco(DynamicPropertyRegistry configuracao) {
        configuracao.add("spring.datasource.url", MYSQL::getJdbcUrl);
        configuracao.add("spring.datasource.username", MYSQL::getUsername);
        configuracao.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired com.devannalu.tsworkspace.projetos.ProjetoService projetos;
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
        jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM task"); jdbc.update("DELETE FROM project");
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
    private Cookie entrar(String usuario) throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM app_user WHERE id=?", String.class, usuario);
        return http.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", SENHA_TESTE))))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("TS_SESSION");
    }

    private ProjetoService.ProjetoResponse projeto(String autora,String equipe,List<String> responsaveis) {
        return projetos.criarProjeto(autora,"  Encontro  ","Objetivo",equipe,responsaveis,LocalDate.now(),LocalDate.now().plusDays(3));
    }
    private ProjetoService.ProjetoResponse editarProjeto(String autora,ProjetoService.ProjetoResponse p,Projeto.Status status,String equipe,List<String> ids) {
        return projetos.editarProjeto(autora,p.id(),p.titulo(),p.descricao(),status,equipe,ids,p.dataInicio(),p.dataFim(),p.versao());
    }
    private TarefaService.TarefaResponse tarefa(ProjetoService.ProjetoResponse p) {
        return tarefas.criarTarefa(admin,"Preparar encontro",null,null,p.equipe().id(),null,List.of(),p.id());
    }
    private ProjetoService.FiltrosProjetos filtros(String equipe,Projeto.Status status,String pessoa,String busca,boolean arquivo,int pagina,int tamanho) {
        return new ProjetoService.FiltrosProjetos(equipe,status,pessoa,busca,null,null,null,null,arquivo,pagina,tamanho);
    }
    @Test void deveAplicarV9ComVinculoOpcionalESeedIdempotente() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("16");assertThat(flyway.migrate().migrationsExecuted).isZero();
        seed.seed();seed.seed();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permissions",Integer.class)).isEqualTo(42);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions",Integer.class)).isEqualTo(118);
        assertThat(criar(admin,equipeA,"Standalone",List.of()).projeto()).isNull();
        assertThat(jdbc.queryForObject("SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='task' AND COLUMN_NAME='project_id'",String.class)).isEqualTo("YES");
    }
    @Test void deveCriarPlanejadoComCriadoraDaSessaoESemTarefas() {
        var p=projeto(supervisora,equipeA,List.of(suporte));
        assertThat(p.titulo()).isEqualTo("Encontro");assertThat(p.status()).isEqualTo(Projeto.Status.PLANEJADO);
        assertThat(p.criadaPor().id()).isEqualTo(supervisora);assertThat(p.totalTarefas()).isZero();assertThat(p.percentualProgresso()).isNull();
        assertThat(p.responsaveis()).hasSize(1);assertThat(p.capacidades().arquivar()).isFalse();
    }
    @Test void deveExigirEquipeAtivaResponsaveisElegiveisEPeriodo() {
        assertThatThrownBy(()->projeto(admin,equipeA,List.of(fora))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->projetos.criarProjeto(admin,"Titulo",null,equipeA,List.of(),LocalDate.now(),LocalDate.now().minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP WHERE id=?",equipeA);
        assertThatThrownBy(()->projeto(admin,equipeA,List.of())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void deveAplicarMatrizSemAlterarPermissoesDeTarefas() {
        for(String pessoa:List.of(superAdmin,admin,supervisora,suporte))assertThat(permissoes.possuiPermissao(pessoa,"projects.view")).isTrue();
        for(String chave:List.of("projects.create","projects.edit","projects.manage_members")) {
            assertThat(permissoes.possuiPermissao(supervisora,chave)).isTrue();assertThat(permissoes.possuiPermissao(suporte,chave)).isFalse();
        }
        assertThat(permissoes.possuiPermissao(supervisora,"projects.archive")).isFalse();assertThat(permissoes.possuiPermissao(admin,"projects.archive")).isTrue();
        assertThat(permissoes.possuiPermissao(suporte,"tasks.create")).isTrue();
        assertThatThrownBy(()->projeto(suporte,equipeA,List.of())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void deveRestringirEquipeSemBypassDaResponsabilidade() {
        var p=projeto(admin,equipeB,List.of(fora));
        assertThatThrownBy(()->projetos.buscarProjeto(supervisora,p.id())).isInstanceOf(AccessDeniedException.class);
        assertThat(projetos.listarProjetos(suporte,filtros(null,null,null,null,false,0,24)).items()).isEmpty();
        assertThat(projetos.buscarProjeto(admin,p.id()).id()).isEqualTo(p.id());
        assertThatThrownBy(()->projeto(supervisora,equipeB,List.of())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void deveAplicarOverrideENegacaoComBypassSuperAdmin() {
        jdbc.update("INSERT INTO user_permissions (user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key='projects.create'",admin);
        assertThatThrownBy(()->projeto(admin,equipeA,List.of())).isInstanceOf(AccessDeniedException.class);
        jdbc.update("INSERT INTO user_permissions (user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key='projects.create'",superAdmin);
        assertThat(projeto(superAdmin,equipeA,List.of()).status()).isEqualTo(Projeto.Status.PLANEJADO);
    }
    @Test void deveCalcularProgressoRealSemConcluirAutomaticamente() {
        var p=projeto(admin,equipeA,List.of());var t=tarefa(p);
        assertThat(projetos.buscarProjeto(admin,p.id()).percentualProgresso()).isZero();
        assertThatThrownBy(()->editarProjeto(admin,projetos.buscarProjeto(admin,p.id()),Projeto.Status.CONCLUIDO,equipeA,List.of())).hasMessageContaining("pendentes antes de concluir");
        t=tarefas.moverTarefa(admin,t.id(),Tarefa.Status.CONCLUIDA,null,t.versao());
        var atualizado=projetos.buscarProjeto(admin,p.id());assertThat(atualizado.percentualProgresso()).isEqualTo(100);
        assertThat(atualizado.status()).isEqualTo(Projeto.Status.PLANEJADO);
        tarefas.arquivarTarefa(admin,t.id(),t.versao());assertThat(projetos.buscarProjeto(admin,p.id()).percentualProgresso()).isNull();
    }
    @Test void deveBloquearVinculosEReaberturaAteReabrirProjeto() {
        var p=projeto(admin,equipeA,List.of());var t=tarefa(p);t=tarefas.moverTarefa(admin,t.id(),Tarefa.Status.CONCLUIDA,null,t.versao());
        p=editarProjeto(admin,projetos.buscarProjeto(admin,p.id()),Projeto.Status.CONCLUIDO,equipeA,List.of());
        final var concluido=p; final var tarefaConcluida=t;
        assertThatThrownBy(()->tarefa(concluido)).hasMessageContaining("Reabra o projeto");
        assertThatThrownBy(()->tarefas.moverTarefa(admin,tarefaConcluida.id(),Tarefa.Status.A_FAZER,null,tarefaConcluida.versao())).hasMessageContaining("Reabra o projeto");
        p=editarProjeto(admin,p,Projeto.Status.EM_ANDAMENTO,equipeA,List.of());assertThat(tarefa(p).projeto().id()).isEqualTo(p.id());
    }
    @Test void deveConcluirProjetoVazioEPermitirPausarComTarefas() {
        var p=projeto(admin,equipeA,List.of());p=editarProjeto(admin,p,Projeto.Status.CONCLUIDO,equipeA,List.of());
        p=editarProjeto(admin,p,Projeto.Status.PAUSADO,equipeA,List.of());var t=tarefa(p);
        assertThat(tarefas.moverTarefa(admin,t.id(),Tarefa.Status.EM_ANDAMENTO,null,t.versao()).status()).isEqualTo(Tarefa.Status.EM_ANDAMENTO);
    }
    @Test void deveArquivarSomenteSemPendenciasPreservandoTarefas() {
        var p=projeto(admin,equipeA,List.of());var t=tarefa(p);var atual=projetos.buscarProjeto(admin,p.id());
        assertThatThrownBy(()->projetos.arquivarProjeto(admin,atual.id(),atual.versao())).hasMessageContaining("pendentes antes de arquivar");
        t=tarefas.moverTarefa(admin,t.id(),Tarefa.Status.CONCLUIDA,null,t.versao());p=projetos.arquivarProjeto(admin,atual.id(),atual.versao());
        assertThat(p.arquivado()).isTrue();assertThat(tarefas.buscarTarefa(admin,t.id()).arquivada()).isFalse();
        assertThat(projetos.listarProjetos(admin,filtros(null,null,null,null,false,0,24)).items()).isEmpty();
        assertThat(projetos.listarProjetos(admin,filtros(null,null,null,null,true,0,24)).items()).hasSize(1);
        final var arquivo=p;assertThatThrownBy(()->tarefa(arquivo)).hasMessageContaining("Projeto arquivado");
    }
    @Test void deveTrocarEquipeSomenteAntesDeQualquerVinculoSemRemoverResponsaveisSilenciosamente() {
        var p=projeto(admin,equipeA,List.of(suporte));final var original=p;
        assertThatThrownBy(()->editarProjeto(admin,original,Projeto.Status.PLANEJADO,equipeB,null)).isInstanceOf(IllegalArgumentException.class);
        p=editarProjeto(admin,p,Projeto.Status.PLANEJADO,equipeB,List.of(fora));var t=tarefa(p);
        tarefas.editarTarefa(admin,t.id(),t.titulo(),null,t.prioridade(),equipeB,null,List.of(),t.versao(),null);
        var historico=projetos.buscarProjeto(admin,p.id());
        assertThat(historico.totalTarefas()).isZero();assertThat(historico.capacidades().trocarEquipe()).isFalse();
        assertThatThrownBy(()->editarProjeto(admin,historico,Projeto.Status.PLANEJADO,equipeA,List.of())).hasMessageContaining("já teve tarefas vinculadas");
    }
    @Test void deveVincularTrocarRemoverEFiltrarTarefasComMesmaEquipe() {
        var a=projeto(admin,equipeA,List.of());var b=projeto(admin,equipeA,List.of());var externo=projeto(admin,equipeB,List.of());var t=tarefa(a);
        final var inicial=t;
        assertThatThrownBy(()->tarefas.editarTarefa(admin,inicial.id(),inicial.titulo(),null,inicial.prioridade(),equipeA,null,List.of(),inicial.versao(),externo.id())).hasMessageContaining("mesma equipe");
        t=tarefas.editarTarefa(admin,t.id(),t.titulo(),null,t.prioridade(),equipeA,null,List.of(),t.versao(),b.id());
        var filtro=new TarefaService.FiltrosTarefas(null,null,null,null,null,null,null,false,0,25,b.id());
        assertThat(tarefas.listarTarefas(admin,filtro).items()).hasSize(1);
        t=tarefas.editarTarefa(admin,t.id(),t.titulo(),null,t.prioridade(),equipeA,null,List.of(),t.versao(),null);assertThat(t.projeto()).isNull();
        assertThat(tarefas.listarTarefas(admin,filtro).items()).isEmpty();
    }
    @Test void deveBloquearEdicaoHistoricaEmEquipeArquivada() {
        var p=projeto(admin,equipeA,List.of());jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP WHERE id=?",equipeA);
        assertThat(projetos.buscarProjeto(admin,p.id()).capacidades().editar()).isFalse();
        assertThatThrownBy(()->editarProjeto(admin,p,Projeto.Status.PAUSADO,equipeA,List.of())).hasMessageContaining("Equipe arquivada");
    }
    @Test void deveProtegerResponsaveisNaRemocaoDeIntegrante() {
        var p=projeto(admin,equipeA,List.of(suporte));
        assertThatThrownBy(()->equipes.removerIntegrante(equipeA,suporte)).hasMessageContaining("projetos ativos");
        assertThatThrownBy(()->usuarios.editarUsuario(admin,suporte,null,null,List.of())).hasMessageContaining("projetos ativos");
        projetos.arquivarProjeto(admin,p.id(),p.versao());equipes.removerIntegrante(equipeA,suporte);
    }
    @Test void deveFiltrarPaginarEBuscarTextoLiteral() {
        var p=projeto(admin,equipeA,List.of(suporte));projeto(admin,equipeB,List.of());
        assertThat(projetos.listarProjetos(admin,filtros(equipeA,Projeto.Status.PLANEJADO,suporte,"Objetivo",false,0,1)).total()).isEqualTo(1);
        assertThat(projetos.listarProjetos(admin,filtros(null,null,null,"%",false,0,24)).items()).isEmpty();
        assertThat(projetos.listarProjetos(admin,filtros(null,null,null,null,false,1,1)).items()).hasSize(1);
        assertThat(projetos.resumirProjetos(suporte).projetosAtivos()).isEqualTo(1);
        assertThat(projetos.resumirProjetos(suporte).comPrazoProximo()).isEqualTo(1);
        assertThat(projetos.buscarOpcoes(suporte,equipeA).responsaveis()).hasSize(3);
    }
    @Test void deveRetornar401403404409EValidarCsrfSemExporSegredos() throws Exception {
        http.perform(get("/api/v1/projects")).andExpect(status().isUnauthorized());
        var p=projeto(admin,equipeA,List.of());var cookie=entrar(suporte);
        http.perform(get("/api/v1/projects/"+p.id()).cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.capacidades.editar").value(false))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
        http.perform(get("/api/v1/projects/"+UUID.randomUUID()).cookie(cookie)).andExpect(status().isNotFound());
        var dados=Map.of("titulo","Alterado","status","PAUSADO","equipeId",equipeA,"versao",p.versao());
        http.perform(patch("/api/v1/projects/"+p.id()).cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados))).andExpect(status().isForbidden());
        http.perform(patch("/api/v1/projects/"+p.id()).cookie(entrar(admin)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados))).andExpect(status().isForbidden());
        editarProjeto(admin,p,Projeto.Status.PAUSADO,equipeA,List.of());
        http.perform(patch("/api/v1/projects/"+p.id()).cookie(entrar(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Este projeto foi atualizado por outra pessoa. Atualize os dados e tente novamente."));
    }
    @Test void deveAuditarApenasMudancasRelevantes() {
        var p=projeto(admin,equipeA,List.of());p=editarProjeto(admin,p,Projeto.Status.EM_ANDAMENTO,equipeA,List.of(suporte));
        projetos.arquivarProjeto(admin,p.id(),p.versao());
        assertThat(jdbc.queryForList("SELECT action FROM audit_log WHERE entity_id=? ORDER BY created_at",String.class,p.id()))
            .containsExactly("project.created","project.responsibles_changed","project.status_changed","project.archived");
    }
    @Test void deveAceitarUmaEdicaoConcorrenteERejeitarOutra() throws Exception {
        var p=projeto(admin,equipeA,List.of());var inicio=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        Callable<String> editar=()->{inicio.await();try{editarProjeto(admin,p,Projeto.Status.PAUSADO,equipeA,List.of());return "ok";}
            catch(ProblemaDominio erro){return "conflito";}};
        try {var a=executor.submit(editar);var b=executor.submit(editar);inicio.countDown();
            assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder("ok","conflito");
        } finally {executor.shutdownNow();}
        assertThat(projetos.buscarProjeto(admin,p.id()).versao()).isEqualTo(1);
    }
}
