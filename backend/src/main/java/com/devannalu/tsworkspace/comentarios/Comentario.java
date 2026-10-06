package com.devannalu.tsworkspace.comentarios;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workspace_comment")
public class Comentario {
    @Id @Column(length = 36) private String id;
    @Column(name = "task_id", length = 36) private String tarefaId;
    @Column(name = "project_id", length = 36) private String projetoId;
    @Column(name = "author_id", nullable = false, length = 36) private String autoraId;
    @Column(name = "content", nullable = false, columnDefinition = "TEXT") private String conteudo;
    @Version @Column(name = "version", nullable = false) private long versao;
    @Column(name = "created_at", nullable = false) private Instant criadaEm;
    @Column(name = "updated_at", nullable = false) private Instant atualizadaEm;
    @Column(name = "removed_at") private Instant removidaEm;
    @Column(name = "removed_by_id", length = 36) private String removidaPorId;
    protected Comentario() { }
}
