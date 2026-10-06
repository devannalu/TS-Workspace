package com.devannalu.tsworkspace.projetos;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "project")
public class Projeto {
    public enum Status { PLANEJADO, EM_ANDAMENTO, PAUSADO, CONCLUIDO }
    @Id @Column(length = 36) private String id;
    @Column(name = "title", nullable = false, length = 200) private String titulo;
    @Column(name = "description", columnDefinition = "TEXT") private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(name = "team_id", nullable = false, length = 36) private String equipeId;
    @Column(name = "created_by_id", nullable = false, length = 36) private String criadaPorId;
    @Column(name = "start_date") private LocalDate dataInicio;
    @Column(name = "end_date") private LocalDate dataFim;
    @Column(name = "version", nullable = false) private long versao;
    @Column(name = "first_task_linked_at") private Instant primeiroVinculoTarefaEm;
    @Column(name = "archived_at") private Instant arquivadaEm;
    @Column(name = "created_at", nullable = false) private Instant criadaEm;
    @Column(name = "updated_at", nullable = false) private Instant atualizadaEm;
    protected Projeto() { }
}
