package com.devannalu.tsworkspace.projetos;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity @Table(name = "project_responsible") @IdClass(ResponsavelProjeto.Chave.class)
public class ResponsavelProjeto {
    @Id @Column(name = "project_id", length = 36) private String projetoId;
    @Id @Column(name = "user_id", length = 36) private String usuarioId;
    protected ResponsavelProjeto() { }
    public static class Chave implements Serializable {
        public String projetoId;
        public String usuarioId;
        public Chave() { }
        @Override public boolean equals(Object outro) {
            return outro instanceof Chave chave && Objects.equals(projetoId, chave.projetoId)
                && Objects.equals(usuarioId, chave.usuarioId);
        }
        @Override public int hashCode() { return Objects.hash(projetoId, usuarioId); }
    }
}
