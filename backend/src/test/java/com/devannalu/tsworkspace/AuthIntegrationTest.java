package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.auth.BootstrapService;
import com.devannalu.tsworkspace.auth.Profile;
import com.devannalu.tsworkspace.auth.ProfileRepository;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import com.devannalu.tsworkspace.auth.User;
import com.devannalu.tsworkspace.auth.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_auth_test")
        .withLabel("com.tsworkspace.purpose", "auth-foundation-test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserRepository users;
    @Autowired ProfileRepository profiles;
    @Autowired BootstrapService bootstrap;

    @BeforeEach
    void resetData() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM app_profile");
        jdbc.update("DELETE FROM app_user");
    }

    @Test
    void validLoginPersistsSessionAndMeOmitsHash() throws Exception {
        User user = createUser("Test User", " Test@Example.COM ", "correct horse battery staple");
        Csrf csrf = csrf();

        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                .cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\" TEST@example.com \",\"password\":\"correct horse battery staple\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId()))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("TS_SESSION=")))
            .andReturn();

        Cookie session = login.getResponse().getCookie("TS_SESSION");
        assertThat(session).isNotNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isEqualTo(1);
        mvc.perform(get("/api/v1/auth/me").cookie(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test User"))
            .andExpect(jsonPath("$.jobTitle").doesNotExist());
    }

    @Test
    void invalidPasswordAndUnknownEmailAreGeneric401() throws Exception {
        createUser("Test User", "known@example.com", "correct horse battery staple");
        Csrf csrf = csrf();
        String body = "{\"email\":\"known@example.com\",\"password\":\"wrong password\"}";
        String wrong = mvc.perform(post("/api/v1/auth/login").cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(post("/api/v1/auth/login").cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"missing@example.com\",\"password\":\"wrong password\"}"))
            .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(wrong).contains("Credenciais inválidas.");
        assertThat(missing).contains("Credenciais inválidas.");
        assertThat(wrong).doesNotContain("known@example.com");
        assertThat(wrong).doesNotContain("passwordHash");
    }

    @Test
    void logoutInvalidatesSessionAndMeRequiresAuthentication() throws Exception {
        createUser("Test User", "logout@example.com", "correct horse battery staple");
        Csrf csrf = csrf();
        MvcResult login = login("logout@example.com", "correct horse battery staple", csrf);
        Cookie session = login.getResponse().getCookie("TS_SESSION");
        mvc.perform(post("/api/v1/auth/logout").cookie(session, csrf.cookie()).header("X-XSRF-TOKEN", csrf.token()))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isZero();
    }

    @Test
    void inactiveUserCannotLoginAndExistingSessionIsInvalidated() throws Exception {
        User user = createUser("Test User", "inactive@example.com", "correct horse battery staple");
        Csrf csrf = csrf();
        MvcResult login = login("inactive@example.com", "correct horse battery staple", csrf);
        Cookie session = login.getResponse().getCookie("TS_SESSION");
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?", user.getId());
        mvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isZero();

        Csrf secondCsrf = csrf();
        mvc.perform(post("/api/v1/auth/login").cookie(secondCsrf.cookie()).header("X-XSRF-TOKEN", secondCsrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"inactive@example.com\",\"password\":\"correct horse battery staple\"}"))
            .andExpect(status().isUnauthorized()).andExpect(content().string(org.hamcrest.Matchers.containsString("Credenciais inválidas.")));
    }

    @Test
    void csrfIsRequiredAndOriginIsRestricted() throws Exception {
        createUser("Test User", "csrf@example.com", "correct horse battery staple");
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"csrf@example.com\",\"password\":\"correct horse battery staple\"}"))
            .andExpect(status().isForbidden());
        Csrf csrf = csrf();
        mvc.perform(options("/api/v1/auth/csrf").header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(get("/api/v1/auth/csrf").header("Origin", "https://example.invalid"))
            .andExpect(status().isForbidden());
        assertThat(csrf.token()).isNotBlank();
    }

    @Test
    void bootstrapIsIdempotentAndRejectsPartialState() {
        bootstrap.createFirstIdentity("Bootstrap User", "Bootstrap@Example.COM", "correct horse battery staple");
        bootstrap.createFirstIdentity("Different Name", "bootstrap@example.com", "another password that is long");
        assertThat(users.findByEmail("bootstrap@example.com")).isPresent();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_profile", Integer.class)).isEqualTo(1);
        User partial = users.save(new User("Partial", "partial@example.com", passwordEncoder.encode("correct horse battery staple")));
        assertThatThrownBy(() -> bootstrap.createFirstIdentity("Partial", "partial@example.com", "correct horse battery staple"))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("parcial");
        assertThat(profiles.findById(partial.getId())).isEmpty();
    }

    private User createUser(String name, String email, String password) {
        User user = users.save(new User(name, email.trim().toLowerCase(), passwordEncoder.encode(password)));
        profiles.save(new Profile(user.getId(), ProfileStatus.ACTIVE));
        return user;
    }

    private MvcResult login(String email, String password, Csrf csrf) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").cookie(csrf.cookie()).header("X-XSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn();
    }

    private Csrf csrf() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return new Csrf(body.get("token").asText(), cookie);
    }

    private record Csrf(String token, Cookie cookie) { }
}
