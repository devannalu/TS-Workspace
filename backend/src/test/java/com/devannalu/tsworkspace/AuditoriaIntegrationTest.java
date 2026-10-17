package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.autenticacao.InicializacaoIdentidadeService;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.rbac.*;
import com.devannalu.tsworkspace.notas.NotaRepository.Recurso;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
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
class AuditoriaIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_auditoria_test").withLabel("com.tsworkspace.purpose", "auditoria-test");
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
    @Autowired com.devannalu.tsworkspace.parcerias.ParceriaService parcerias;
    @Autowired com.devannalu.tsworkspace.notas.NotaService notas;
    @Autowired com.devannalu.tsworkspace.auditoria.AuditoriaService auditoria;
    @Autowired com.devannalu.tsworkspace.equipes.EquipeService equipes;
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
    private void negar(String pessoa, String permissao) {
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ?,id,'DENY' FROM permissions WHERE permission_key=?", pessoa, permissao);
    }
    private Cookie entrar(String usuario) throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM app_user WHERE id=?", String.class, usuario);
        return http.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email", email, "password", SENHA_TESTE))))
            .andExpect(status().isOk()).andReturn().getResponse().getCookie("TS_SESSION");
    }

    private com.devannalu.tsworkspace.auditoria.AuditoriaService.Pagina listar(String pessoa,String autora,String acao,String tipo,Instant desde,Instant ate,int pagina,int tamanho) {
        return auditoria.listar(pessoa,autora,acao,tipo,desde,ate,pagina,tamanho);
    }
    private String registrar(String acao,String tipo,String entidade,String metadados,Instant instante) {
        String id=UUID.randomUUID().toString();jdbc.update("INSERT INTO audit_log(id,actor_id,action,entity_type,entity_id,metadata_json,created_at) VALUES(?,?,?,?,?,?,?)",id,admin,acao,tipo,entidade,metadados,java.sql.Timestamp.from(instante));return id;
    }
    @Test void consultaSomenteLeituraComFiltrosEPaginacao() {
        for(int i=0;i<7;i++)registrar("task.created","Task",UUID.randomUUID().toString(),null,Instant.now().minusSeconds(60+i));
        registrar("project.created","Project",UUID.randomUUID().toString(),null,Instant.now().minusSeconds(40));
        var pagina=listar(superAdmin,"ADMIN","task.created","Task",null,null,1,3);
        assertThat(pagina.total()).isEqualTo(7);assertThat(pagina.items()).hasSize(3);
        assertThat(pagina.items()).allMatch(e->e.autora().id().equals(admin)&&e.acao().equals("task.created"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log",Integer.class)).isEqualTo(8);
    }
    @Test void periodoUsaInicioInclusivoEFimExclusivo() {
        Instant inicio=Instant.parse("2026-10-01T00:00:00Z"),fim=inicio.plusSeconds(60);
        registrar("task.created","Task",UUID.randomUUID().toString(),null,inicio);
        registrar("task.created","Task",UUID.randomUUID().toString(),null,fim);
        assertThat(listar(admin,null,null,null,inicio,fim,0,25).total()).isEqualTo(1);
        assertThatThrownBy(()->listar(admin,null,null,null,inicio,inicio.plus(Duration.ofDays(367)),0,25)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void detalheNaoExibeMetadadosEmailConteudoOuIdentificadorLivre() throws Exception {
        String id=registrar("task.created","Task","senha-nao-pode-aparecer","{\"token\":\"segredo-privado\",\"textoRemovido\":\"conteudo-privado\"}",Instant.now());
        var detalhe=auditoria.detalhe(admin,id);assertThat(detalhe.recursoId()).isNull();
        String serializado=json.writeValueAsString(detalhe);
        assertThat(serializado).doesNotContain("segredo-privado","conteudo-privado","metadata","senha-nao-pode-aparecer","@example.test");
        assertThat(serializado).contains("Teste ADMIN","task.created");
        var desconhecida=registrar("segredo-na-acao","segredo-no-tipo",UUID.randomUUID().toString(),null,Instant.now());
        assertThat(auditoria.detalhe(admin,desconhecida).acao()).isEqualTo("UNKNOWN");assertThat(auditoria.detalhe(admin,desconhecida).tipo()).isEqualTo("UNKNOWN");
    }
    @Test void permissaoAtualEOverrideSaoAutoridade() {
        assertThatThrownBy(()->listar(suporte,null,null,null,null,null,0,25)).isInstanceOf(AccessDeniedException.class);
        jdbc.update("INSERT INTO user_permissions(user_id,permission_id,effect) SELECT ?,id,'ALLOW' FROM permissions WHERE permission_key='audit.view'",supervisora);
        assertThat(listar(supervisora,null,null,null,null,null,0,25).total()).isZero();
        negar(admin,"audit.view");assertThatThrownBy(()->auditoria.opcoes(admin)).isInstanceOf(AccessDeniedException.class);
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?",supervisora);
        assertThatThrownBy(()->auditoria.opcoes(supervisora)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void equipesRegistramAutoraSemCorpoEArquivoDuplicado() {
        var equipe=equipes.criarEquipe(admin,"Equipe auditada","Informação privada",equipeA).team().id();
        equipes.editarEquipe(admin,equipe,"Equipe editada","Informação privada revisada",equipeA);
        equipes.adicionarIntegrante(admin,equipe,suporte);equipes.removerIntegrante(admin,equipe,suporte);
        equipes.arquivarEquipe(admin,equipe);equipes.arquivarEquipe(admin,equipe);
        var eventos=listar(admin,null,null,"Team",null,null,0,25);
        assertThat(eventos.items()).hasSize(5).allMatch(e->e.autora().id().equals(admin)&&e.recursoId().equals(equipe));
        assertThat(eventos.items()).extracting(e->e.acao()).containsExactlyInAnyOrder("team.created","team.updated","team.member_added","team.member_removed","team.archived");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE metadata_json IS NOT NULL",Integer.class)).isZero();
    }
    @Test void limitesERegistroInexistente() {
        assertThatThrownBy(()->listar(admin,null,null,null,null,null,0,101)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->listar(admin,null,"x' OR 1=1",null,null,null,0,25)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->auditoria.detalhe(admin,UUID.randomUUID().toString())).isInstanceOf(ProblemaDominio.class);
        assertThat(auditoria.opcoes(admin).acoes()).contains("team.created","note.removed");
    }
    @Test void contratoHttpBloqueiaSessaoPermissaoEMutacoes() throws Exception {
        http.perform(get("/api/v1/audit")).andExpect(status().isUnauthorized());
        var semAcesso=entrar(suporte);http.perform(get("/api/v1/audit").cookie(semAcesso)).andExpect(status().isForbidden());
        var sessao=entrar(admin);http.perform(get("/api/v1/audit").cookie(sessao)).andExpect(status().isOk());
        http.perform(get("/api/v1/audit/"+UUID.randomUUID()).cookie(sessao)).andExpect(status().isNotFound());
        http.perform(post("/api/v1/audit").cookie(sessao).with(csrf())).andExpect(status().isForbidden());
        http.perform(get("/api/v1/audit").cookie(sessao).param("size","101")).andExpect(status().isBadRequest());
    }
}
