package com.devannalu.tsworkspace.tarefas;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "task")
public class Tarefa {
    public enum Status { A_FAZER, EM_ANDAMENTO, EM_REVISAO, CONCLUIDA }
    public enum Prioridade { BAIXA, MEDIA, ALTA, URGENTE }

    @Id @Column(length = 36) private String id;
    @Column(name = "title", nullable = false, length = 200) private String titulo;
    @Column(name = "description", columnDefinition = "TEXT") private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Enumerated(EnumType.STRING) @Column(name = "priority", nullable = false, length = 10) private Prioridade prioridade;
    @Column(name = "team_id", nullable = false, length = 36) private String equipeId;
    @Column(name = "created_by_id", nullable = false, length = 36) private String criadaPorId;
    @Column(name = "due_date") private LocalDate prazo;
    @Column(name = "position", nullable = false) private int ordem;
    @Column(name = "version", nullable = false) private long versao;
    @Column(name = "archived_at") private Instant arquivadaEm;
    @Column(name = "created_at", nullable = false) private Instant criadaEm;
    @Column(name = "updated_at", nullable = false) private Instant atualizadaEm;
    protected Tarefa() { }
}
