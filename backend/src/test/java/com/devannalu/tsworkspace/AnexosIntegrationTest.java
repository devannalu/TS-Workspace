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
class AnexosIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_anexos_test").withLabel("com.tsworkspace.purpose", "anexos-test");
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
    @Autowired com.devannalu.tsworkspace.anexos.AnexoService anexos;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.devannalu.tsworkspace.anexos.ArmazenamentoArquivo storage;
    static final com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso TAREFA = com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso.TAREFA;
    static final com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso PROJETO = com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso.PROJETO;
    String superAdmin, admin, supervisora, suporte, outra, fora, equipeA, equipeB;
    static final String SENHA_TESTE = "Senha apenas de teste isolado 2026";

    @BeforeEach void prepararBancoIsolado() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES"); jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM attachment"); jdbc.update("DELETE FROM audit_log"); jdbc.update("DELETE FROM workspace_comment"); jdbc.update("DELETE FROM task"); jdbc.update("DELETE FROM project");
        jdbc.update("DELETE FROM team_member"); jdbc.update("DELETE FROM user_permissions");
        jdbc.update("DELETE FROM app_profile"); jdbc.update("DELETE FROM app_user");
        jdbc.update("UPDATE team SET archived_at=NULL");
        org.mockito.Mockito.when(storage.validade()).thenReturn(Duration.ofMinutes(5));
        org.mockito.Mockito.when(storage.criarUpload(org.mockito.ArgumentMatchers.any())).thenReturn(
            new com.devannalu.tsworkspace.anexos.ArmazenamentoArquivo.Upload("https://storage.example.test/upload",Map.of("Content-Type","application/pdf")));
        org.mockito.Mockito.when(storage.gerarDownload(org.mockito.ArgumentMatchers.any())).thenReturn("https://storage.example.test/download");
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
    private TarefaService.TarefaResponse tarefa() { return criar(superAdmin, equipeA, "Conversa", List.of(suporte)); }
    private com.devannalu.tsworkspace.anexos.AnexoService.UploadResponse iniciar(String autora,String id) {
        return anexos.criarUpload(autora,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,id,"documento.pdf","application/pdf",12);
    }
    @Test void confirmaIdempotenteAuditaListaBaixaRemove() {
        var t=tarefa();var u=iniciar(suporte,t.id());
        assertThat(anexos.listar(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),0,25).items()).isEmpty();
        var a=anexos.confirmar(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),u.attachmentId());
        assertThat(anexos.confirmar(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),a.id()).id()).isEqualTo(a.id());
        org.mockito.Mockito.verify(storage,org.mockito.Mockito.times(1)).confirmarUpload(org.mockito.ArgumentMatchers.any());
        assertThat(anexos.download(suporte,a.id()).expiresAt()).isBefore(Instant.now().plusSeconds(301));
        assertThat(comentarios.listarAtividade(suporte,TAREFA,t.id(),0,25).items()).extracting(e->e.tipo()).contains("attachment.created");
        anexos.remover(suporte,a.id());anexos.remover(suporte,a.id());
        assertThatThrownBy(()->anexos.download(suporte,a.id())).isInstanceOf(ProblemaDominio.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action='attachment.removed'",Long.class)).isEqualTo(1);
    }
    @Test void verificaPermissaoEscopoAutoriaEModeracao() {
        var t=tarefa();var u=iniciar(suporte,t.id());
        assertThatThrownBy(()->iniciar(fora,t.id())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->anexos.confirmar(outra,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),u.attachmentId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->anexos.remover(outra,u.attachmentId())).isInstanceOf(AccessDeniedException.class);
        anexos.remover(admin,u.attachmentId());
    }
    @Test void recusaTipoTamanhoETraversalEChavesSaoUnicas() {
        var t=tarefa();
        for(String nome:List.of("../../arquivo.pdf","a.exe","a.html","a.svg"))
            assertThatThrownBy(()->anexos.criarUpload(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),nome,"application/pdf",12)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->anexos.criarUpload(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),"a.pdf","application/pdf",10485761)).isInstanceOf(ProblemaDominio.class);
        iniciar(suporte,t.id());iniciar(suporte,t.id());
        assertThat(jdbc.queryForList("SELECT object_key FROM attachment",String.class)).hasSize(2).doesNotHaveDuplicates().allMatch(k->k.startsWith("tasks/"+t.id()+"/"));
    }
    @Test void confirmacaoContextualExpiracaoEObjetoAusente() {
        var t=tarefa();var outraTarefa=tarefa();var u=iniciar(suporte,t.id());
        assertThatThrownBy(()->anexos.confirmar(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,outraTarefa.id(),u.attachmentId())).isInstanceOf(ProblemaDominio.class);
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT)).when(storage).confirmarUpload(org.mockito.ArgumentMatchers.any());
        assertThatThrownBy(()->anexos.confirmar(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),u.attachmentId())).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT state FROM attachment WHERE id=?",String.class,u.attachmentId())).isEqualTo("PENDENTE");
        jdbc.update("UPDATE attachment SET upload_expires_at=? WHERE id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(60)),u.attachmentId());
        assertThatThrownBy(()->anexos.confirmar(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),u.attachmentId())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void falhaRemocaoMantemBloqueioDeDownloadEPermiteRepetir() {
        var t=tarefa();var u=iniciar(suporte,t.id());anexos.confirmar(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),u.attachmentId());
        org.mockito.Mockito.doThrow(new IllegalStateException("offline")).doNothing().when(storage).removerObjeto(org.mockito.ArgumentMatchers.any());
        assertThatThrownBy(()->anexos.remover(suporte,u.attachmentId())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->anexos.download(suporte,u.attachmentId())).isInstanceOf(ProblemaDominio.class);
        anexos.remover(suporte,u.attachmentId());
    }

    @Test void projetoPermiteSupervisoraERecusaSuporte() {
        var p=projeto(admin,equipeA,List.of());var recurso=com.devannalu.tsworkspace.anexos.Anexo.Recurso.PROJETO;
        var u=anexos.criarUpload(supervisora,recurso,p.id(),"a.pdf","application/pdf",12);
        anexos.confirmar(supervisora,recurso,p.id(),u.attachmentId());
        assertThat(anexos.listar(suporte,recurso,p.id(),0,25).items()).hasSize(1);
        assertThat(anexos.listar(suporte,recurso,p.id(),0,25).podeAnexar()).isFalse();
        assertThatThrownBy(()->anexos.criarUpload(suporte,recurso,p.id(),"a.pdf","application/pdf",12)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void arquivamentoMantemLeituraEBloqueiaColaboracaoNormal() {
        var t=tarefa();var u=iniciar(suporte,t.id());anexos.confirmar(suporte,com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA,t.id(),u.attachmentId());
        jdbc.update("UPDATE task SET archived_at=CURRENT_TIMESTAMP(6) WHERE id=?",t.id());
        assertThat(anexos.download(suporte,u.attachmentId())).isNotNull();
        assertThatThrownBy(()->iniciar(suporte,t.id())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->anexos.remover(suporte,u.attachmentId())).isInstanceOf(AccessDeniedException.class);
        anexos.remover(admin,u.attachmentId());
    }
    @Test void equipeArquivadaEBloqueioExplicitoDeGrant() {
        var t=tarefa();jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP(6) WHERE id=?",equipeA);
        assertThatThrownBy(()->iniciar(suporte,t.id())).isInstanceOf(AccessDeniedException.class);
        jdbc.update("UPDATE team SET archived_at=NULL WHERE id=?",equipeA);
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key='tasks.attach'",suporte);
        assertThatThrownBy(()->iniciar(suporte,t.id())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void httpProtegeSessaoContextoEChaveDoCliente() throws Exception {
        var t=tarefa();http.perform(get("/api/v1/tasks/"+t.id()+"/attachments")).andExpect(status().isUnauthorized());
        var sessao=entrar(suporte);
        http.perform(post("/api/v1/tasks/"+t.id()+"/attachments/upload").cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("nomeOriginal","a.exe","tipoMime","application/pdf","tamanhoBytes",12,"objectKey","evil"))))
            .andExpect(status().isConflict());
        http.perform(post("/api/v1/attachments/"+UUID.randomUUID()+"/download").cookie(sessao).with(csrf())).andExpect(status().isNotFound());
    }

    @Test void autorizacaoRepetidaReutilizaRegistroERecusaAlterarArquivo() {
        var t=tarefa();var solicitacao=UUID.randomUUID();var recurso=com.devannalu.tsworkspace.anexos.Anexo.Recurso.TAREFA;
        var primeira=anexos.criarUpload(suporte,recurso,t.id(),"a.pdf","application/pdf",12,solicitacao);
        var segunda=anexos.criarUpload(suporte,recurso,t.id(),"a.pdf","application/pdf",12,solicitacao);
        assertThat(segunda.attachmentId()).isEqualTo(primeira.attachmentId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM attachment",Long.class)).isEqualTo(1);
        assertThatThrownBy(()->anexos.criarUpload(suporte,recurso,t.id(),"outro.pdf","application/pdf",12,solicitacao)).isInstanceOf(ProblemaDominio.class);
    }
}
