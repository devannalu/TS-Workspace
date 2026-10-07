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
class ComentariosIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_comentarios_test").withLabel("com.tsworkspace.purpose", "comentarios-test");
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
    @Autowired com.devannalu.tsworkspace.comentarios.ComentarioService comentarios;
    static final com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso TAREFA = com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso.TAREFA;
    static final com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso PROJETO = com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso.PROJETO;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM workspace_comment"); jdbc.update("DELETE FROM task"); jdbc.update("DELETE FROM project");
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

    private TarefaService.TarefaResponse tarefa() { return criar(superAdmin, equipeA, "Conversa", List.of(suporte)); }
    private com.devannalu.tsworkspace.comentarios.ComentarioService.ComentarioResponse comentar(String usuaria,String id,String texto) {
        return comentarios.criar(usuaria,TAREFA,id,texto);
    }
    @Test void migrationConstraintSeedEHistoricoPreservados() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("12");
        assertThat(flyway.migrate().migrationsExecuted).isZero(); seed.seed(); seed.seed();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permissions",Integer.class)).isEqualTo(30);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions",Integer.class)).isEqualTo(82);
        var tarefa=tarefa(); var projeto=projeto(superAdmin,equipeA,List.of());
        for(var ids:List.of(Arrays.asList(null,null),Arrays.asList(tarefa.id(),projeto.id()))) {
            assertThatThrownBy(()->jdbc.update("INSERT INTO workspace_comment(id,task_id,project_id,author_id,content,created_at,updated_at) VALUES(?,?,?,?,?,NOW(6),NOW(6))",UUID.randomUUID().toString(),ids.get(0),ids.get(1),superAdmin,"Texto"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class).hasRootCauseMessage("Check constraint 'workspace_comment_resource_ck' is violated.");
        }
    }
    @Test void criaNosDoisRecursosComAutoraETextoSimples() {
        var tarefa=tarefa(); var c=comentar(suporte,tarefa.id(),"  <script>alert(1)</script>\n@Ana **texto**  ");
        assertThat(c.autora().id()).isEqualTo(suporte); assertThat(c.conteudo()).isEqualTo("<script>alert(1)</script>\n@Ana **texto**");
        var p=projeto(superAdmin,equipeA,List.of());
        assertThat(comentarios.criar(supervisora,PROJETO,p.id(),"Projeto").conteudo()).isEqualTo("Projeto");
    }
    @Test void validaConteudoEPaginacao() {
        var t=tarefa(); for(String texto:List.of("","  ","x".repeat(5001))) assertThatThrownBy(()->comentar(suporte,t.id(),texto)).isInstanceOf(IllegalArgumentException.class);
        assertThat(comentar(suporte,t.id(),"x".repeat(5000)).conteudo()).hasSize(5000);
        assertThatThrownBy(()->comentarios.listar(suporte,TAREFA,t.id(),-1,25)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->comentarios.listar(suporte,TAREFA,t.id(),0,101)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void exigeEscopoEPermissaoAtual() {
        var t=tarefa();assertThatThrownBy(()->comentar(fora,t.id(),"Não")).isInstanceOf(AccessDeniedException.class);
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key='tasks.comment'",suporte);
        assertThat(comentarios.listar(suporte,TAREFA,t.id(),0,25).podeComentar()).isFalse();
        assertThatThrownBy(()->comentar(suporte,t.id(),"Não")).isInstanceOf(AccessDeniedException.class);
    }
    @Test void suporteNaoComentaProjetoMesmoComOverride() {
        var p=projeto(superAdmin,equipeA,List.of());
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ?,id,'ALLOW' FROM permissions WHERE permission_key='projects.comment'",suporte);
        assertThat(comentarios.listar(suporte,PROJETO,p.id(),0,25).podeComentar()).isFalse();
        assertThatThrownBy(()->comentarios.criar(suporte,PROJETO,p.id(),"Não")).isInstanceOf(AccessDeniedException.class);
    }
    @Test void editaProprioSemEditarFalaAlheia() {
        var c=comentar(suporte,tarefa().id(),"Original");
        for(String usuaria:List.of(admin,superAdmin,outra)) assertThatThrownBy(()->comentarios.editar(usuaria,c.id(),"Alheio",0)).isInstanceOf(AccessDeniedException.class);
        var editado=comentarios.editar(suporte,c.id(),"Editado",0); assertThat(editado.editado()).isTrue(); assertThat(editado.versao()).isEqualTo(1);
    }
    @Test void removeProprioPreservandoRegistroSemConteudoPublico() {
        var t=tarefa();var c=comentar(suporte,t.id(),"Conteúdo original privado");
        var removido=comentarios.remover(suporte,c.id(),0); assertThat(removido.removido()).isTrue();assertThat(removido.conteudo()).isNull();
        assertThat(comentarios.listar(suporte,TAREFA,t.id(),0,25).items().get(0).conteudo()).isNull();
        assertThat(jdbc.queryForObject("SELECT content FROM workspace_comment WHERE id=?",String.class,c.id())).isEqualTo("Conteúdo original privado");
        assertThat(removido.capacidades().editar()).isFalse();assertThat(removido.capacidades().remover()).isFalse();
    }
    @Test void moderacaoAdministrativaEGrantDeny() {
        var c=comentar(suporte,tarefa().id(),"Fala");
        assertThatThrownBy(()->comentarios.remover(outra,c.id(),0)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->comentarios.remover(supervisora,c.id(),0)).isInstanceOf(AccessDeniedException.class);
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key='comments.moderate'",admin);
        assertThatThrownBy(()->comentarios.remover(admin,c.id(),0)).isInstanceOf(AccessDeniedException.class);
        assertThat(comentarios.remover(superAdmin,c.id(),0).removido()).isTrue();
    }
    @Test void tarefaArquivadaCongelaAutoraMasPermiteModeracao() {
        var t=tarefa();var c=comentar(suporte,t.id(),"Fala");tarefas.arquivarTarefa(superAdmin,t.id(),t.versao());
        assertThat(comentarios.listar(suporte,TAREFA,t.id(),0,25).podeComentar()).isFalse();
        assertThatThrownBy(()->comentar(suporte,t.id(),"Novo")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->comentarios.editar(suporte,c.id(),"Editado",0)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->comentarios.remover(suporte,c.id(),0)).isInstanceOf(AccessDeniedException.class);
        assertThat(comentarios.remover(admin,c.id(),0).removido()).isTrue();
    }
    @Test void projetoArquivadoEEquipeArquivadaSomenteLeitura() {
        var p=projeto(superAdmin,equipeA,List.of());var c=comentarios.criar(supervisora,PROJETO,p.id(),"Fala");
        projetos.arquivarProjeto(superAdmin,p.id(),p.versao());
        assertThatThrownBy(()->comentarios.editar(supervisora,c.id(),"Editado",0)).isInstanceOf(AccessDeniedException.class);
        assertThat(comentarios.remover(admin,c.id(),0).removido()).isTrue();
        var t=tarefa();var ct=comentar(suporte,t.id(),"Fala");jdbc.update("UPDATE team SET archived_at=NOW(6) WHERE id=?",equipeA);
        assertThatThrownBy(()->comentarios.remover(suporte,ct.id(),0)).isInstanceOf(AccessDeniedException.class);
        assertThat(comentarios.remover(admin,ct.id(),0).removido()).isTrue();
    }
    @Test void conclusaoNaoCongelaConversa() {
        var t=tarefa();tarefas.moverTarefa(superAdmin,t.id(),Tarefa.Status.CONCLUIDA,null,t.versao());
        assertThat(comentar(suporte,t.id(),"Concluída").conteudo()).isEqualTo("Concluída");
        var p=projeto(superAdmin,equipeA,List.of());p=editarProjeto(superAdmin,p,Projeto.Status.CONCLUIDO,equipeA,List.of());
        assertThat(comentarios.criar(supervisora,PROJETO,p.id(),"Concluído").conteudo()).isEqualTo("Concluído");
    }
    @Test void conflitoEdicaoERemocaoNaoSobrescreve() {
        var c=comentar(suporte,tarefa().id(),"Original");comentarios.editar(suporte,c.id(),"Vencedor",0);
        assertThatThrownBy(()->comentarios.editar(suporte,c.id(),"Perdedor",0)).isInstanceOf(ProblemaDominio.class).hasMessage(com.devannalu.tsworkspace.comentarios.ComentarioService.CONFLITO);
        assertThatThrownBy(()->comentarios.remover(suporte,c.id(),0)).isInstanceOf(ProblemaDominio.class);
    }
    @Test void paginasSemNmaisUmMantemRemovidos() {
        var t=tarefa(); for(int n=0;n<27;n++)comentar(suporte,t.id(),"Texto "+n);
        var primeira=comentarios.listar(suporte,TAREFA,t.id(),0,25);var segunda=comentarios.listar(suporte,TAREFA,t.id(),1,25);
        assertThat(primeira.total()).isEqualTo(27);assertThat(primeira.items()).hasSize(25);assertThat(segunda.items()).hasSize(2);
        assertThat(primeira.items()).extracting(c->c.id()).doesNotContainAnyElementsOf(segunda.items().stream().map(c->c.id()).toList());
        var c=primeira.items().get(0);comentarios.remover(suporte,c.id(),0);assertThat(comentarios.listar(suporte,TAREFA,t.id(),0,25).total()).isEqualTo(27);
    }
    @Test void atividadeContextualSeguraSemInventarHistorico() {
        var t=tarefa();var outro=tarefa();var c=comentar(suporte,t.id(),"Não deve ir para timeline");
        comentarios.editar(suporte,c.id(),"Texto privado novo",0);comentarios.remover(admin,c.id(),1);
        comentar(suporte,outro.id(),"Outro");
        jdbc.update("INSERT INTO audit_log(id,actor_id,action,entity_type,entity_id,metadata_json,created_at) VALUES(?,?,'comment.created','Unknown',?,JSON_OBJECT('taskId',?,'text','privado'),NOW(6))",UUID.randomUUID().toString(),suporte,c.id(),t.id());
        var eventos=comentarios.listarAtividade(suporte,TAREFA,t.id(),0,25);
        assertThat(eventos.items()).extracting(e->e.tipo()).contains("task.created","comment.created","comment.updated","comment.removed");
        assertThat(eventos.items().stream().filter(e->e.tipo().equals("comment.created"))).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE entity_type='Comment' AND metadata_json IS NOT NULL",Integer.class)).isZero();
        assertThatThrownBy(()->comentarios.listarAtividade(fora,TAREFA,t.id(),0,25)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void atividadeProjetoPaginadaEVazia() {
        var p=projeto(superAdmin,equipeA,List.of());comentarios.criar(supervisora,PROJETO,p.id(),"Fala");
        var primeira=comentarios.listarAtividade(suporte,PROJETO,p.id(),0,1);
        assertThat(primeira.items()).hasSize(1);assertThat(primeira.total()).isEqualTo(2);
        assertThat(comentarios.listarAtividade(suporte,PROJETO,p.id(),1,1).items()).hasSize(1);
        jdbc.update("DELETE FROM audit_log");assertThat(comentarios.listarAtividade(suporte,PROJETO,p.id(),0,25).items()).isEmpty();
    }
    @Test void httpAutoriaCsrfStatusEContratoSeguro() throws Exception {
        var t=tarefa();var cookie=entrar(suporte);String rota="/api/v1/tasks/"+t.id()+"/comments";
        http.perform(get(rota)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Autenticação necessária."));
        http.perform(post(rota).cookie(cookie).contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Fala\"}")).andExpect(status().isForbidden());
        var resposta=http.perform(post(rota).cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Fala\",\"autoraId\":\"ignorada\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.autora.id").value(suporte)).andReturn().getResponse().getContentAsString();
        String id=json.readTree(resposta).get("id").asText();
        http.perform(patch("/api/v1/comments/"+id).cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Nova\",\"versao\":0}")).andExpect(status().isOk());
        http.perform(post("/api/v1/comments/"+id+"/remove").cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"versao\":0}")).andExpect(status().isConflict());
        http.perform(post("/api/v1/comments/"+id+"/remove").cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"versao\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo").doesNotExist()).andExpect(jsonPath("$.removidaPorId").doesNotExist());
        http.perform(get("/api/v1/tasks/"+UUID.randomUUID()+"/comments").cookie(cookie)).andExpect(status().isNotFound());
        http.perform(get(rota).cookie(entrar(fora))).andExpect(status().isForbidden());
        http.perform(post(rota).cookie(cookie).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\" \"}")).andExpect(status().isBadRequest());
    }
}
