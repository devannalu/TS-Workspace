package com.devannalu.tsworkspace.autenticacao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevogacaoSessaoService {
    private final JdbcTemplate jdbc;
    public RevogacaoSessaoService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Transactional
    public void revogarSessoesDoUsuario(String userId) {
        jdbc.update("DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME=(SELECT email FROM app_user WHERE id=?)",userId);
    }
}
