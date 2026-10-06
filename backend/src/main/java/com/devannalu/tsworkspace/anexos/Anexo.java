package com.devannalu.tsworkspace.anexos;

import java.time.Instant;

public record Anexo(String id, String tarefaId, String projetoId, String enviadaPorId, String enviadaPorNome,
    String nomeOriginal, String chaveObjeto, String tipoMime, long tamanhoBytes, Estado estado,
    Instant criadaEm, Instant uploadExpiraEm, Instant removidaEm) {
    public enum Estado { PENDENTE, DISPONIVEL, REMOVIDO }
    public enum Recurso {
        TAREFA("task_id", "tasks"), PROJETO("project_id", "projects");
        final String coluna, rota;
        Recurso(String coluna, String rota) { this.coluna=coluna; this.rota=rota; }
    }
    public Recurso recurso() { return tarefaId!=null ? Recurso.TAREFA : Recurso.PROJETO; }
    public String recursoId() { return tarefaId!=null ? tarefaId : projetoId; }
}
