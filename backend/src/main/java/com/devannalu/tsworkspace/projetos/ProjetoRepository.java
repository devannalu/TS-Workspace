package com.devannalu.tsworkspace.projetos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.devannalu.tsworkspace.projetos.PoliticaProjeto.Acesso;
import com.devannalu.tsworkspace.projetos.ProjetoService.FiltrosProjetos;

@Repository
public class ProjetoRepository {
    private final JdbcTemplate jdbc;
    public ProjetoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Referencia(String id, String nome) { }
    public record EstadoProjeto(String id, String titulo, String descricao, Projeto.Status status,
        Referencia equipe, boolean equipeArquivada, Referencia criadaPor, LocalDate dataInicio, LocalDate dataFim,
        long versao, Instant primeiroVinculoTarefaEm, Instant arquivadaEm, Instant criadaEm, Instant atualizadaEm,
        long totalTarefas, long tarefasConcluidas, long emAndamento, long emRevisao, long aFazer) { }
    public record Consulta(String sql, List<Object> parametros) { }
    // Agregamos uma vez por consulta; nenhum card dispara consultas de tarefas.
    private static final String SQL_PROJETO = """
        SELECT p.*,e.name equipe_nome,e.archived_at equipe_arquivada,u.name criadora_nome,
        COALESCE(t.total,0) total,COALESCE(t.concluidas,0) concluidas,
        COALESCE(t.andamento,0) andamento,COALESCE(t.revisao,0) revisao,COALESCE(t.afazer,0) afazer
        FROM project p JOIN team e ON e.id=p.team_id JOIN app_user u ON u.id=p.created_by_id
        LEFT JOIN (SELECT project_id,COUNT(*) total,SUM(status='CONCLUIDA') concluidas,
        SUM(status='EM_ANDAMENTO') andamento,SUM(status='EM_REVISAO') revisao,SUM(status='A_FAZER') afazer
        FROM task WHERE archived_at IS NULL AND project_id IS NOT NULL GROUP BY project_id) t ON t.project_id=p.id
        """;
    private static Instant instante(ResultSet linha, String campo) throws SQLException {
        var valor = linha.getTimestamp(campo); return valor == null ? null : valor.toInstant();
    }
    private static LocalDate data(ResultSet linha, String campo) throws SQLException {
        var valor = linha.getDate(campo); return valor == null ? null : valor.toLocalDate();
    }
    private static EstadoProjeto lerProjeto(ResultSet l, int indice) throws SQLException {
        return new EstadoProjeto(l.getString("id"),l.getString("title"),l.getString("description"),
            Projeto.Status.valueOf(l.getString("status")),new Referencia(l.getString("team_id"),l.getString("equipe_nome")),
            l.getTimestamp("equipe_arquivada") != null,new Referencia(l.getString("created_by_id"),l.getString("criadora_nome")),
            data(l,"start_date"),data(l,"end_date"),l.getLong("version"),instante(l,"first_task_linked_at"),
            instante(l,"archived_at"),instante(l,"created_at"),instante(l,"updated_at"),l.getLong("total"),
            l.getLong("concluidas"),l.getLong("andamento"),l.getLong("revisao"),l.getLong("afazer"));
    }
    private static void escopo(StringBuilder sql, List<Object> parametros, Acesso acesso) {
        if (!acesso.global()) {
            sql.append(" AND EXISTS (SELECT 1 FROM team_member own WHERE own.team_id=p.team_id AND own.user_id=?)");
            parametros.add(acesso.usuarioId());
        }
    }
    public Consulta montarConsulta(Acesso acesso, FiltrosProjetos filtros) {
        var sql = new StringBuilder(filtros.arquivados() ? " WHERE p.archived_at IS NOT NULL" : " WHERE p.archived_at IS NULL");
        List<Object> args = new ArrayList<>(); escopo(sql,args,acesso);
        if (filtros.equipeId()!=null) { sql.append(" AND p.team_id=?"); args.add(filtros.equipeId()); }
        if (filtros.status()!=null) { sql.append(" AND p.status=?"); args.add(filtros.status().name()); }
        if (filtros.responsavelId()!=null) {
            sql.append(" AND EXISTS (SELECT 1 FROM project_responsible r WHERE r.project_id=p.id AND r.user_id=?)"); args.add(filtros.responsavelId());
        }
        if (filtros.busca()!=null && !filtros.busca().isBlank()) {
            sql.append(" AND (p.title LIKE ? ESCAPE '!' OR p.description LIKE ? ESCAPE '!')");
            String busca="%"+filtros.busca().trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            args.add(busca); args.add(busca);
        }
        if (filtros.inicioDe()!=null) { sql.append(" AND p.start_date>=?"); args.add(filtros.inicioDe()); }
        if (filtros.inicioAte()!=null) { sql.append(" AND p.start_date<=?"); args.add(filtros.inicioAte()); }
        if (filtros.fimDe()!=null) { sql.append(" AND p.end_date>=?"); args.add(filtros.fimDe()); }
        if (filtros.fimAte()!=null) { sql.append(" AND p.end_date<=?"); args.add(filtros.fimAte()); }
        return new Consulta(sql.toString(),args);
    }
    public List<EstadoProjeto> listar(Consulta consulta, int pagina, int tamanho) {
        List<Object> args=new ArrayList<>(consulta.parametros()); args.add(tamanho); args.add(pagina*tamanho);
        return jdbc.query(SQL_PROJETO+consulta.sql()+" ORDER BY p.created_at DESC,p.id LIMIT ? OFFSET ?",ProjetoRepository::lerProjeto,args.toArray());
    }
    public long contar(Consulta consulta) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM project p"+consulta.sql(),Long.class,consulta.parametros().toArray());
    }
    public Optional<EstadoProjeto> buscar(String id) {
        return jdbc.query(SQL_PROJETO+" WHERE p.id=?",ProjetoRepository::lerProjeto,id).stream().findFirst();
    }
    public Map<String,List<Referencia>> buscarResponsaveis(Collection<String> ids) {
        Map<String,List<Referencia>> resultado=new HashMap<>(); if(ids.isEmpty())return resultado;
        String marcadores=String.join(",",Collections.nCopies(ids.size(),"?"));
        jdbc.query("SELECT r.project_id,u.id,u.name FROM project_responsible r JOIN app_user u ON u.id=r.user_id WHERE r.project_id IN ("+marcadores+") ORDER BY u.name,u.id",
            (org.springframework.jdbc.core.RowCallbackHandler) l -> resultado.computeIfAbsent(l.getString("project_id"),chave->new ArrayList<>())
                .add(new Referencia(l.getString("id"),l.getString("name"))),ids.toArray());
        return resultado;
    }
    public void inserir(String id,String titulo,String descricao,String equipeId,String criadoraId,LocalDate inicio,LocalDate fim) {
        jdbc.update("INSERT INTO project (id,title,description,status,team_id,created_by_id,start_date,end_date,version,created_at,updated_at) VALUES (?,?,?,'PLANEJADO',?,?,?,?,0,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",
            id,titulo,descricao,equipeId,criadoraId,inicio,fim);
    }
    public int atualizar(EstadoProjeto atual,String titulo,String descricao,Projeto.Status status,String equipeId,LocalDate inicio,LocalDate fim) {
        return jdbc.update("UPDATE project SET title=?,description=?,status=?,team_id=?,start_date=?,end_date=?,version=version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=? AND version=?",
            titulo,descricao,status.name(),equipeId,inicio,fim,atual.id(),atual.versao());
    }
    public void substituirResponsaveis(String id,Set<String> ids) {
        jdbc.update("DELETE FROM project_responsible WHERE project_id=?",id);
        for(String usuario:ids)jdbc.update("INSERT INTO project_responsible (project_id,user_id) VALUES (?,?)",id,usuario);
    }
    public int arquivar(EstadoProjeto atual) {
        return jdbc.update("UPDATE project SET archived_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6),version=version+1 WHERE id=? AND version=?",atual.id(),atual.versao());
    }
    public void registrarPrimeiroVinculo(String id) {
        // Este marco permanece após desvincular a tarefa e impede mudança de equipe histórica.
        jdbc.update("UPDATE project SET first_task_linked_at=CURRENT_TIMESTAMP(6),version=version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=? AND first_task_linked_at IS NULL",id);
    }
    public boolean possuiResponsabilidadeAtiva(String usuarioId,String equipeId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM project_responsible r JOIN project p ON p.id=r.project_id WHERE r.user_id=? AND p.team_id=? AND p.archived_at IS NULL",Long.class,usuarioId,equipeId)>0;
    }
    public ProjetoService.ResumoProjetos resumir(Acesso acesso,LocalDate hoje) {
        var sql=new StringBuilder(" WHERE p.archived_at IS NULL"); List<Object> args=new ArrayList<>(List.of(hoje,hoje.plusDays(7)));escopo(sql,args,acesso);
        return jdbc.queryForObject("SELECT COUNT(*) ativos,COALESCE(SUM(p.status='EM_ANDAMENTO'),0) andamento,COALESCE(SUM(p.status<>'CONCLUIDO' AND p.end_date BETWEEN ? AND ?),0) proximos FROM project p"+sql,
            (l,i)->new ProjetoService.ResumoProjetos(l.getLong("ativos"),l.getLong("andamento"),l.getLong("proximos")),args.toArray());
    }
}
