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
class BuscaIntegrationTest {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_busca_test").withLabel("com.tsworkspace.purpose", "busca-test");
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
    @Autowired com.devannalu.tsworkspace.busca.BuscaService busca;
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
    private String projeto() {return projeto(equipeA,null,null,"PLANEJADO",false);}
    private com.devannalu.tsworkspace.busca.BuscaService.Resposta buscar(String pessoa,String termo,com.devannalu.tsworkspace.busca.BuscaRepository.Tipo tipo) {
        return busca.buscar(pessoa,termo,tipo,0,20);
    }
    @Test void agrupaRecursosENotasSemExporContato() {
        var parceria=parcerias.criar(supervisora,dados());
        var projeto=projeto();
        jdbc.update("UPDATE project SET title='Busca sisters' WHERE id=?",projeto);
        jdbc.update("UPDATE partnership SET organization='Busca sisters',contact_email='segredo-contato@example.test' WHERE id=?",parceria.id());
        var nota=notas.criar(supervisora,Recurso.PROJETO,projeto,"Busca sisters","Decisão documentada");
        var resposta=buscar(suporte,"Busca sisters",null);
        assertThat(resposta.grupos().stream().mapToLong(g->g.total()).sum()).isEqualTo(3);
        var grupo=resposta.grupos().stream().filter(g->g.tipo()==com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.NOTA).findFirst().orElseThrow();
        assertThat(grupo.items().get(0).rota()).isEqualTo("/projetos?abrir="+projeto+"&nota="+nota.id());
        assertThat(buscar(superAdmin,"segredo-contato",null).grupos().stream().mapToLong(g->g.total()).sum()).isZero();
    }
    @Test void revalidaEquipePermissaoEArquivamentoAntesDaContagem() {
        var projeto=projeto();jdbc.update("UPDATE project SET title='Busca sisters' WHERE id=?",projeto);
        assertThat(buscar(fora,"Busca",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.PROJETO).grupos().get(0).total()).isZero();
        assertThat(buscar(admin,"Busca",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.PROJETO).grupos().get(0).total()).isEqualTo(1);
        negar(suporte,"projects.view");
        assertThatThrownBy(()->buscar(suporte,"Busca",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.PROJETO)).isInstanceOf(AccessDeniedException.class);
        jdbc.update("UPDATE team SET archived_at=NOW(6) WHERE id=?",equipeA);
        assertThat(buscar(admin,"Busca",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.PROJETO).grupos().get(0).total()).isZero();
    }
    @Test void notaRemovidaOuFonteInacessivelNaoEntraNaBusca() {
        var projeto=projeto();var nota=notas.criar(supervisora,Recurso.PROJETO,projeto,"Busca sisters","Texto");
        assertThat(buscar(suporte,"Busca",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.NOTA).grupos().get(0).total()).isEqualTo(1);
        negar(suporte,"projects.view");
        assertThat(buscar(suporte,"Busca",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.NOTA).grupos().get(0).total()).isZero();
        notas.remover(supervisora,nota.id(),0);
        assertThat(buscar(admin,"Busca",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.NOTA).grupos().get(0).total()).isZero();
        assertThatThrownBy(()->notas.detalhe(admin,Recurso.PROJETO,projeto,nota.id())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void linkDaNotaConfereContextoMesmoQuandoAmbosSaoLegiveis() {
        var projeto=projeto();var outro=projeto();var nota=notas.criar(supervisora,Recurso.PROJETO,projeto,"Busca","Texto");
        assertThat(notas.detalhe(suporte,Recurso.PROJETO,projeto,nota.id()).id()).isEqualTo(nota.id());
        assertThatThrownBy(()->notas.detalhe(suporte,Recurso.PROJETO,outro,nota.id())).isInstanceOf(ProblemaDominio.class);
    }
    @Test void buscaLiteralPaginadaComLimites() {
        for(int i=0;i<7;i++) {var id=projeto();jdbc.update("UPDATE project SET title=? WHERE id=?","Busca 100%_ "+i,id);}
        var resposta=busca.buscar(suporte,"100%_",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.PROJETO,1,3);
        assertThat(resposta.grupos().get(0).total()).isEqualTo(7);assertThat(resposta.grupos().get(0).items()).hasSize(3);
        assertThat(buscar(suporte,"100%X",com.devannalu.tsworkspace.busca.BuscaRepository.Tipo.PROJETO).grupos().get(0).total()).isZero();
        assertThatThrownBy(()->buscar(suporte,"a",null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->busca.buscar(suporte,"Busca",null,0,101)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void contratoHttpExigeSessaoEPermissaoAtual() throws Exception {
        http.perform(get("/api/v1/search").param("q","Busca")).andExpect(status().isUnauthorized());
        var cookie=entrar(suporte);
        http.perform(get("/api/v1/search").cookie(cookie).param("q","Busca")).andExpect(status().isOk());
        http.perform(get("/api/v1/search").cookie(cookie).param("q","a")).andExpect(status().isBadRequest());
        negar(suporte,"projects.view");
        http.perform(get("/api/v1/search").cookie(cookie).param("q","Busca").param("type","PROJETO")).andExpect(status().isForbidden());
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?",suporte);
        http.perform(get("/api/v1/search").cookie(cookie).param("q","Busca")).andExpect(status().isForbidden());
    }
}
