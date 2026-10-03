package com.devannalu.tsworkspace.usuarios;

import com.devannalu.tsworkspace.auth.ProfileStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import com.devannalu.tsworkspace.rbac.PerfilAcesso;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "app_profile")
public class Perfil {
    @Id
    @Column(name = "user_id", length = 36, updatable = false)
    private String usuarioId;

    @Column(name = "job_title", length = 160)
    private String cargo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private PerfilAcesso perfilAcesso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProfileStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant criadoEm;

    @Column(name = "updated_at", nullable = false)
    private Instant atualizadoEm;

    protected Perfil() { }

    public Perfil(String usuarioId, ProfileStatus status, PerfilAcesso perfilAcesso) {
        this.usuarioId = usuarioId;
        this.status = status;
        this.perfilAcesso = java.util.Objects.requireNonNull(perfilAcesso);
    }

    @PrePersist
    void registrarCriacao() {
        Instant agora = Instant.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void registrarAtualizacao() { atualizadoEm = Instant.now(); }

    public String getUsuarioId() { return usuarioId; }
    public String getCargo() { return cargo; }
    public ProfileStatus getStatus() { return status; }
    public PerfilAcesso getPerfilAcesso() { return perfilAcesso; }
}
