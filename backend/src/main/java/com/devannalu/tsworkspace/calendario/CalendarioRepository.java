package com.devannalu.tsworkspace.calendario;

import com.devannalu.tsworkspace.tarefas.TarefaRepository;
import com.devannalu.tsworkspace.projetos.ProjetoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CalendarioRepository {
    private final JdbcTemplate jdbc;
    public CalendarioRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Registro(String id, String titulo, LocalDate inicio, LocalDate fim,
        String equipeId, String equipeNome, String status, String prioridade, boolean possuiFim) { }

    public List<Registro> tarefas(TarefaRepository.Consulta consulta) {
        return jdbc.query("SELECT t.id,t.title,t.due_date inicio,t.due_date fim,t.team_id,e.name equipe_nome,t.status,t.priority FROM task t JOIN team e ON e.id=t.team_id"
            + consulta.sql() + " ORDER BY t.due_date,t.id", (linha, indice) -> new Registro(
                linha.getString("id"), linha.getString("title"), linha.getObject("inicio", LocalDate.class),
                linha.getObject("fim", LocalDate.class), linha.getString("team_id"), linha.getString("equipe_nome"),
                linha.getString("status"), linha.getString("priority"), true), consulta.parametros().toArray());
    }

    public List<Registro> projetos(ProjetoRepository.Consulta consulta, LocalDate de, LocalDate ate) {
        var parametros = new ArrayList<>(consulta.parametros());
        parametros.add(ate); parametros.add(de);
        // COALESCE representa uma data isolada como um dia; períodos conservam ambas as pontas.
        return jdbc.query("SELECT p.id,p.title,COALESCE(p.start_date,p.end_date) inicio,COALESCE(p.end_date,p.start_date) fim,p.team_id,e.name equipe_nome,p.status,p.end_date FROM project p JOIN team e ON e.id=p.team_id"
            + consulta.sql() + " AND COALESCE(p.start_date,p.end_date)<=? AND COALESCE(p.end_date,p.start_date)>=? ORDER BY inicio,p.id",
            (linha, indice) -> new Registro(linha.getString("id"), linha.getString("title"),
                linha.getObject("inicio", LocalDate.class), linha.getObject("fim", LocalDate.class),
                linha.getString("team_id"), linha.getString("equipe_nome"), linha.getString("status"), null, linha.getObject("end_date") != null), parametros.toArray());
    }
}
