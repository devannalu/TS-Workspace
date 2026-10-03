package com.devannalu.tsworkspace.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class Usuario {
    @Id
    @Column(length = 36, updatable = false)
    private String id;

    @Column(name = "name", nullable = false, length = 160)
    private String nome;

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String hashSenha;

    @Column(name = "created_at", nullable = false)
    private Instant criadoEm;

    @Column(name = "updated_at", nullable = false)
    private Instant atualizadoEm;

    protected Usuario() { }

    public Usuario(String nome, String email, String hashSenha) {
        this.nome = nome;
        this.email = email;
        this.hashSenha = hashSenha;
    }

    @PrePersist
    void registrarCriacao() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant agora = Instant.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void registrarAtualizacao() { atualizadoEm = Instant.now(); }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getHashSenha() { return hashSenha; }
}
