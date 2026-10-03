package com.devannalu.tsworkspace.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionRevocationService {
    private final JdbcTemplate jdbc;
    public SessionRevocationService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Transactional
    public void revokeAll(String userId) {
        jdbc.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME=(SELECT email FROM app_user WHERE id=?)",userId);
    }
}
