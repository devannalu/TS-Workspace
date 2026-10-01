package com.devannalu.tsworkspace;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FoundationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_java_test")
        .withLabel("com.tsworkspace.purpose", "foundation-test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test
    void contextMigratesAndValidatesRealMysql() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(jdbc.queryForObject("SELECT name FROM schema_marker WHERE id = 1", String.class)).isEqualTo("java-foundation");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM schema_marker", Integer.class)).isEqualTo(1);
    }

    @Test
    void healthReadsDatabaseAndExposesOnlyStatus() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test
    void allowsConfiguredFrontendOrigin() throws Exception {
        mvc.perform(get("/api/v1/health").header("Origin", "http://localhost:3000"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/v1/health").header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk());
    }

    @Test
    void rejectsForeignOrigin() throws Exception {
        mvc.perform(get("/api/v1/health").header("Origin", "https://example.invalid"))
            .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void csrfAndDefaultDenialRemainEnabled() throws Exception {
        mvc.perform(post("/api/v1/health")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }
}
