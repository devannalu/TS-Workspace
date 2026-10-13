package com.devannalu.tsworkspace.busca;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BuscaRepository {
    private final JdbcTemplate jdbc;
    public BuscaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public enum Tipo {
        TAREFA("task", "title", "description", "tasks.view", "/tarefas", null),
        PROJETO("project", "title", "description", "projects.view", "/projetos", "project_id"),
        REUNIAO("meeting", "title", "agenda", "meetings.view", "/reunioes", "meeting_id"),
        EVENTO("community_event", "name", "description", "events.view", "/eventos", "event_id"),
        CONTEUDO("communication_content", "title", "briefing", "content.view", "/conteudos", null),
        PARCERIA("partnership", "organization", "description", "partnerships.view", "/parcerias", "partnership_id"),
        NOTA(null, null, null, null, null, null);
        final String tabela, titulo, texto, permissao, rota, colunaNota;
        Tipo(String tabela, String titulo, String texto, String permissao, String rota, String colunaNota) {
            this.tabela=tabela; this.titulo=titulo; this.texto=texto; this.permissao=permissao; this.rota=rota; this.colunaNota=colunaNota;
        }
    }
    public record Resultado(String id, String titulo, String resumo, String contexto, String rota) { }
    public record Pagina(List<Resultado> items, long total) { }
    private record Consulta(String sql, List<Object> parametros) { }
    private Consulta consulta(Tipo tipo, String pessoa, boolean global, String termo, Set<String> permissoes) {
        var consultas=new ArrayList<String>(); var parametros=new ArrayList<Object>();
        var fontes=tipo==Tipo.NOTA?Arrays.stream(Tipo.values()).filter(t->t.colunaNota!=null&&permissoes.contains(t.permissao)).toList():List.of(tipo);
        for(var fonte:fontes) {
            boolean nota=tipo==Tipo.NOTA;
            String titulo=nota?"n.title":"r."+fonte.titulo, texto=nota?"n.body":"r."+fonte.texto;
            String rota=nota?"CONCAT('"+fonte.rota+"?abrir=',r.id,'&nota=',n.id)":"CONCAT('"+fonte.rota+"?abrir=',r.id)";
            var sql=new StringBuilder("SELECT "+(nota?"n.id":"r.id")+" id,"+titulo+" titulo,LEFT("+texto+",200) resumo,"+(nota?"r."+fonte.titulo:"e.name")+" contexto,"+rota+" rota FROM "+fonte.tabela+" r JOIN team e ON e.id=r.team_id ");
            if(nota) sql.append("JOIN workspace_note n ON n.").append(fonte.colunaNota).append("=r.id AND n.removed_at IS NULL ");
            sql.append("WHERE r.archived_at IS NULL AND e.archived_at IS NULL AND (").append(titulo).append(" LIKE ? ESCAPE '!' OR ").append(texto).append(" LIKE ? ESCAPE '!')");
            parametros.add(termo); parametros.add(termo);
            if(!global) {sql.append(" AND EXISTS(SELECT 1 FROM team_member m WHERE m.team_id=r.team_id AND m.user_id=?)");parametros.add(pessoa);}
            consultas.add(sql.toString());
        }
        return new Consulta(String.join(" UNION ALL ",consultas),parametros);
    }
    public Pagina buscar(Tipo tipo,String pessoa,boolean global,String busca,Set<String> permissoes,int pagina,int tamanho) {
        String termo="%"+busca.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        var consulta=consulta(tipo,pessoa,global,termo,permissoes);
        if(consulta.sql().isEmpty())return new Pagina(List.of(),0);
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM ("+consulta.sql()+") resultados",Long.class,consulta.parametros().toArray());
        var parametros=new ArrayList<>(consulta.parametros());parametros.add(tamanho);parametros.add(pagina*tamanho);
        var items=jdbc.query("SELECT * FROM ("+consulta.sql()+") resultados ORDER BY titulo,id LIMIT ? OFFSET ?",
            (r,i)->new Resultado(r.getString("id"),r.getString("titulo"),r.getString("resumo"),r.getString("contexto"),r.getString("rota")),parametros.toArray());
        return new Pagina(items,total);
    }
}
