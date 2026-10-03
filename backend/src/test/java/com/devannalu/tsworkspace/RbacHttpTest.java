package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.autenticacao.InicializacaoIdentidadeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

/** Real TCP HTTP and JDBC sessions; test identities never enter the development database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RbacHttpTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_rbac_http_test").withLabel("com.tsworkspace.purpose", "rbac-http-test");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @LocalServerPort int port;
    @Autowired InicializacaoIdentidadeService bootstrap;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    private static final String PASSWORD = "HTTP RBAC test password only";

    @Test void realHttpEnforcesSessionCsrfRoleAndPermissionAndLogout() throws Exception {
        bootstrap.criarPrimeiraIdentidade("HTTP Admin Test", "http-admin@example.test", PASSWORD);
        bootstrap.criarPrimeiraIdentidade("HTTP Support Test", "http-support@example.test", PASSWORD);
        jdbc.update("UPDATE app_profile SET role_id=(SELECT id FROM roles WHERE role_key='SUPPORT') WHERE user_id=(SELECT id FROM app_user WHERE email='http-support@example.test')");
        HttpClient admin = client();
        assertThat(get(admin, "/api/v1/health").statusCode()).isEqualTo(200);
        assertThat(get(admin, "/api/v1/permissions").statusCode()).isEqualTo(401);
        assertThat(post(admin, "/api/v1/auth/login", "", "http-admin@example.test").statusCode()).isEqualTo(403);
        String csrf = csrf(admin);
        var login = post(admin, "/api/v1/auth/login", csrf, "http-admin@example.test");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.headers().allValues("set-cookie").stream().anyMatch(value -> value.startsWith("TS_SESSION=") && value.toLowerCase().contains("httponly"))).isTrue();
        var me = get(admin, "/api/v1/auth/me");
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(me.body()).path("role").path("key").asText()).isEqualTo("SUPER_ADMIN");
        assertThat(mapper.readTree(me.body()).has("passwordHash")).isFalse();
        var catalog = get(admin, "/api/v1/permissions");
        assertThat(catalog.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(catalog.body()).size()).isEqualTo(14);
        HttpClient support = client();
        assertThat(post(support, "/api/v1/auth/login", csrf(support), "http-support@example.test").statusCode()).isEqualTo(200);
        assertThat(get(support, "/api/v1/permissions").statusCode()).isEqualTo(403);
        assertThat(get(support, "/api/v1/auth/me").statusCode()).isEqualTo(200);
        assertThat(post(support, "/api/v1/auth/logout", csrf(support), null).statusCode()).isEqualTo(204);
        assertThat(post(admin, "/api/v1/auth/logout", csrf(admin), null).statusCode()).isEqualTo(204);
        assertThat(get(admin, "/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isZero();
    }
    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private String csrf(HttpClient client) throws Exception {
        var response = get(client, "/api/v1/auth/csrf");
        assertThat(response.statusCode()).isEqualTo(200);
        return mapper.readTree(response.body()).path("token").asText();
    }
    private HttpResponse<String> post(HttpClient client, String path, String csrf, String email) throws Exception {
        String body = email == null ? "{}" : mapper.writeValueAsString(Map.of("email", email, "password", PASSWORD));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path))
            .header("Content-Type", "application/json").header("X-XSRF-TOKEN", csrf)
            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
