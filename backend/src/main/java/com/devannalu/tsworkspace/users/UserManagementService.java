package com.devannalu.tsworkspace.users;

import com.devannalu.tsworkspace.audit.AuditService;
import com.devannalu.tsworkspace.autenticacao.RevogacaoSessaoService;
import com.devannalu.tsworkspace.common.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {
    public record PerfilAcesso(String id,String key,String name) { }
    public record TeamRef(String id,String name) { }
    public record UserDto(String id,String name,String email,String jobTitle,String status,PerfilAcesso role,List<TeamRef> teams,Instant createdAt) { }
    public record Page(List<UserDto> items,long total,int page,int size) { }
    public record Options(List<PerfilAcesso> roles,List<TeamRef> teams) { }
    private final JdbcTemplate jdbc;
    private final OrganizationLock lock;
    private final AuditService audit;
    private final RevogacaoSessaoService sessions;
    public UserManagementService(JdbcTemplate jdbc,OrganizationLock lock,AuditService audit,RevogacaoSessaoService sessions) {
        this.jdbc=jdbc; this.lock=lock; this.audit=audit; this.sessions=sessions;
    }
    private static final String SELECT="SELECT u.id,u.name,u.email,u.created_at,p.job_title,p.status,r.id role_id,r.role_key,r.name role_name FROM app_user u JOIN app_profile p ON p.user_id=u.id JOIN roles r ON r.id=p.role_id";
    private static UserDto row(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
        return new UserDto(rs.getString("id"),rs.getString("name"),rs.getString("email"),rs.getString("job_title"),rs.getString("status"),
            new PerfilAcesso(rs.getString("role_id"),rs.getString("role_key"),rs.getString("role_name")),List.of(),rs.getTimestamp("created_at").toInstant());
    }
    private List<UserDto> withTeams(List<UserDto> users) {
        if(users.isEmpty())return users;
        Map<String,List<TeamRef>> memberships=new HashMap<>();
        var ids=users.stream().map(UserDto::id).toList();
        jdbc.query("SELECT tm.user_id,t.id,t.name FROM team_member tm JOIN team t ON t.id=tm.team_id WHERE t.archived_at IS NULL AND tm.user_id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+") ORDER BY t.name,t.id",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> memberships.computeIfAbsent(rs.getString("user_id"),k->new ArrayList<>()).add(new TeamRef(rs.getString("id"),rs.getString("name"))),ids.toArray());
        return users.stream().map(u->new UserDto(u.id(),u.name(),u.email(),u.jobTitle(),u.status(),u.role(),List.copyOf(memberships.getOrDefault(u.id(),List.of())),u.createdAt())).toList();
    }
    @Transactional(readOnly=true)
    public Page list(int page,int size,String status,String role,String team,String search) {
        if(page<0||page>100000||size<1||size>100)throw new IllegalArgumentException();
        List<Object> args=new ArrayList<>(); String where=" WHERE 1=1";
        if(status!=null){where+=" AND p.status=?";args.add(status);}
        if(role!=null){where+=" AND p.role_id=?";args.add(role);}
        if(team!=null){where+=" AND EXISTS (SELECT 1 FROM team_member tm WHERE tm.user_id=u.id AND tm.team_id=?)";args.add(team);}
        if(search!=null&&!search.isBlank()){if(search.length()>160)throw new IllegalArgumentException();where+=" AND (LOCATE(LOWER(?),LOWER(u.name))>0 OR LOCATE(LOWER(?),LOWER(u.email))>0)";args.add(search.trim());args.add(search.trim());}
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM app_user u JOIN app_profile p ON p.user_id=u.id"+where,Long.class,args.toArray());
        args.add(size);args.add(page*size);
        return new Page(withTeams(jdbc.query(SELECT+where+" ORDER BY u.created_at DESC,u.id LIMIT ? OFFSET ?",UserManagementService::row,args.toArray())),total,page,size);
    }
    @Transactional(readOnly=true)
    public Options options() {
        return new Options(jdbc.query("SELECT id,role_key,name FROM roles ORDER BY name",(rs,n)->new PerfilAcesso(rs.getString("id"),rs.getString("role_key"),rs.getString("name"))),
            jdbc.query("SELECT id,name FROM team WHERE archived_at IS NULL ORDER BY name",(rs,n)->new TeamRef(rs.getString("id"),rs.getString("name"))));
    }
    @Transactional(readOnly=true)
    public UserDto detail(String id) {
        var found=jdbc.query(SELECT+" WHERE u.id=?",UserManagementService::row,id);
        if(found.isEmpty())throw DomainProblem.missing("Usuária ou perfil não encontrado.");
        return withTeams(found).get(0);
    }
    @Transactional
    public UserDto edit(String actor,String id,String jobTitle,String roleId,List<String> teamIds) {
        lock.acquire(); var before=detail(id);
        String nextRole=roleId==null?before.role().id():roleId;
        var roles=jdbc.query("SELECT id,role_key,name FROM roles WHERE id=?",(rs,n)->new PerfilAcesso(rs.getString("id"),rs.getString("role_key"),rs.getString("name")),nextRole);
        if(roles.isEmpty())throw DomainProblem.missing("Cargo não encontrado.");
        if(roleId!=null||teamIds!=null)protect(actor,before,roles.get(0).key(),before.status().equals("ACTIVE"),teamIds);
        if(teamIds!=null){
            Set<String> unique=new LinkedHashSet<>(teamIds);
            if(unique.size()>50)throw new IllegalArgumentException();
            for(String team:unique)if(jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE id=? AND archived_at IS NULL",Long.class,team)==0)throw DomainProblem.conflict("Uma ou mais equipes não estão disponíveis.");
            Set<String> old=new HashSet<>(jdbc.queryForList("SELECT team_id FROM team_member WHERE user_id=?",String.class,id));
            if(!old.equals(unique)){
                for(String team:old)if(!unique.contains(team))jdbc.update("DELETE FROM team_member WHERE user_id=? AND team_id=?",id,team);
                for(String team:unique)if(!old.contains(team))jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))",id,team);
                audit.record(actor,"user.teams_changed","User",id);
            }
        }
        String title=jobTitle==null?before.jobTitle():(jobTitle.trim().isEmpty()?null:jobTitle.trim());
        if(!before.role().id().equals(nextRole))audit.record(actor,"user.role_changed","User",id);
        if(!before.role().id().equals(nextRole)||!Objects.equals(title,before.jobTitle()))
            jdbc.update("UPDATE app_profile SET role_id=?,job_title=?,updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=?",nextRole,title,id);
        return detail(id);
    }
    @Transactional
    public UserDto status(String actor,String id,boolean active) {
        lock.acquire();var before=detail(id);
        protect(actor,before,before.role().key(),active,null);
        String next=active?"ACTIVE":"INACTIVE";
        if(!before.status().equals(next)){
            jdbc.update("UPDATE app_profile SET status=?,updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=?",next,id);
            audit.record(actor,"user.status_changed","User",id);
        }
        if(!active)sessions.revogarSessoesDoUsuario(id);
        return detail(id);
    }
    private void protect(String actor,UserDto before,String nextRole,boolean nextActive,List<String> nextTeams) {
        long admins=jdbc.queryForObject("SELECT COUNT(*) FROM app_profile p JOIN roles r ON r.id=p.role_id WHERE p.status='ACTIVE' AND r.role_key='SUPER_ADMIN'",Long.class);
        UserPolicy.protect(actor,before.id(),before.role().key(),before.status().equals("ACTIVE"),nextRole,nextActive,admins);
        String root=jdbc.queryForObject("SELECT id FROM team WHERE team_key='fundadoras'",String.class);
        boolean member=jdbc.queryForObject("SELECT COUNT(*) FROM team_member WHERE user_id=? AND team_id=?",Long.class,before.id(),root)>0;
        long others=jdbc.queryForObject("SELECT COUNT(*) FROM team_member tm JOIN app_profile p ON p.user_id=tm.user_id JOIN roles r ON r.id=p.role_id WHERE tm.team_id=? AND tm.user_id<>? AND p.status='ACTIVE' AND r.role_key='SUPER_ADMIN'",Long.class,root,before.id());
        UserPolicy.founders(member&&before.status().equals("ACTIVE")&&before.role().key().equals("SUPER_ADMIN"),member&&nextActive&&nextRole.equals("SUPER_ADMIN")&&(nextTeams==null||nextTeams.contains(root)),others);
    }
}
