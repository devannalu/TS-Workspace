package com.devannalu.tsworkspace.tarefas;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "task_assignee")
@IdClass(ResponsavelTarefa.Chave.class)
public class ResponsavelTarefa {
    @Id @Column(name = "task_id", length = 36) private String tarefaId;
    @Id @Column(name = "user_id", length = 36) private String usuarioId;
    protected ResponsavelTarefa() { }

    public static class Chave implements Serializable {
        public String tarefaId;
        public String usuarioId;
        public Chave() { }
        @Override public boolean equals(Object outro) {
            return outro instanceof Chave chave && Objects.equals(tarefaId, chave.tarefaId)
                && Objects.equals(usuarioId, chave.usuarioId);
        }
        @Override public int hashCode() { return Objects.hash(tarefaId, usuarioId); }
    }
}
