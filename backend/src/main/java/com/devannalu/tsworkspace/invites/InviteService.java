package com.devannalu.tsworkspace.invites;

import com.devannalu.tsworkspace.audit.AuditService;
import com.devannalu.tsworkspace.auth.EmailNormalizer;
import com.devannalu.tsworkspace.common.*;
import com.devannalu.tsworkspace.users.UserManagementService;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InviteService {
    public record Actor(String id,String name) { }
    public record InviteDto(String id,String email,UserManagementService.Role role,List<UserManagementService.TeamRef> teams,Actor invitedBy,Instant createdAt,Instant expiresAt,InvitePolicy.Status status) { }
    public record Page(List<InviteDto> items,long total,int page,int size) { }
    public record Created(InviteDto invite,String token,String inviteUrl) { }
    public record PublicInvite(String email,String role,List<String> teams,Instant expiresAt) { }
    public record Accepted(String userId,String inviteId) { }
    private record State(String id,String email,String roleId,Instant expires,Instant used,Instant cancelled) { }
    private final JdbcTemplate jdbc;
    private final OrganizationLock lock;
    private final AuditService audit;
    private final PasswordEncoder passwords;
    private final String frontendOrigin;
    public InviteService(JdbcTemplate jdbc,OrganizationLock lock,AuditService audit,PasswordEncoder passwords,@Value("${app.frontend-origin}") String frontendOrigin) {
        this.jdbc=jdbc;this.lock=lock;this.audit=audit;this.passwords=passwords;this.frontendOrigin=frontendOrigin;
    }
    private static Instant instant(java.sql.ResultSet rs,String key) throws java.sql.SQLException { var value=rs.getTimestamp(key);return value==null?null:value.toInstant(); }
    private List<InviteDto> rows(String suffix,Object...args) {
        var result=jdbc.query("SELECT i.*,r.role_key,r.name role_name,u.name actor_name FROM invite i JOIN roles r ON r.id=i.role_id JOIN app_user u ON u.id=i.invited_by_id "+suffix,(rs,n)->new InviteDto(rs.getString("id"),rs.getString("email"),new UserManagementService.Role(rs.getString("role_id"),rs.getString("role_key"),rs.getString("role_name")),List.of(),new Actor(rs.getString("invited_by_id"),rs.getString("actor_name")),instant(rs,"created_at"),instant(rs,"expires_at"),InvitePolicy.status(instant(rs,"used_at"),instant(rs,"cancelled_at"),instant(rs,"expires_at"),Instant.now())),args);
        if(result.isEmpty())return result;
        Map<String,List<UserManagementService.TeamRef>> byInvite=new HashMap<>();
        jdbc.query("SELECT it.invite_id,t.id,t.name FROM invite_team it JOIN team t ON t.id=it.team_id WHERE it.invite_id IN ("+String.join(",",Collections.nCopies(result.size(),"?"))+") ORDER BY t.name,t.id",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> byInvite.computeIfAbsent(rs.getString("invite_id"),k->new ArrayList<>()).add(new UserManagementService.TeamRef(rs.getString("id"),rs.getString("name"))),result.stream().map(InviteDto::id).toArray());
        return result.stream().map(i->new InviteDto(i.id(),i.email(),i.role(),List.copyOf(byInvite.getOrDefault(i.id(),List.of())),i.invitedBy(),i.createdAt(),i.expiresAt(),i.status())).toList();
    }
    @Transactional(readOnly=true)
    public Page list(int page,int size) {
        if(page<0||page>100000||size<1||size>100)throw new IllegalArgumentException();
        return new Page(rows("ORDER BY i.created_at DESC,i.id LIMIT ? OFFSET ?",size,page*size),jdbc.queryForObject("SELECT COUNT(*) FROM invite",Long.class),page,size);
    }
    @Transactional
    public Created create(String actor,String email,String roleId,List<String> teams) {
        return create(actor,email,roleId,teams,InvitePolicy.TTL_DAYS);
    }
    @Transactional
    public Created create(String actor,String email,String roleId,List<String> teams,int expiresInDays) {
        if(expiresInDays<1||expiresInDays>30)throw new IllegalArgumentException();
        lock.acquire();email=EmailNormalizer.normalize(email);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email=?",Long.class,email)>0)throw DomainProblem.conflict("Já existe uma usuária com este e-mail.");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM invite WHERE email=? AND used_at IS NULL AND cancelled_at IS NULL AND expires_at>CURRENT_TIMESTAMP(6)",Long.class,email)>0)throw DomainProblem.conflict("Já existe um convite pendente para este e-mail.");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE id=?",Long.class,roleId)==0)throw DomainProblem.missing("Cargo não encontrado.");
        Set<String> unique=new LinkedHashSet<>(teams);
        if(unique.isEmpty()||teams.size()>20)throw new IllegalArgumentException();
        validateTeams(unique,false);
        String token=InvitePolicy.token(),id=UUID.randomUUID().toString();
        Instant now=Instant.now(),expires=now.plus(Duration.ofDays(expiresInDays));
        jdbc.update("INSERT INTO invite (id,email,token_hash,expires_at,invited_by_id,role_id,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)",id,email,InvitePolicy.hash(token),Timestamp.from(expires),actor,roleId,Timestamp.from(now),Timestamp.from(now));
        for(String team:unique)jdbc.update("INSERT INTO invite_team VALUES (?,?,?)",id,team,Timestamp.from(now));
        audit.record(actor,"invite.created","Invite",id);
        // Prepared for the future frontend cutover; the preserved Prisma UI does not consume Java invites.
        return new Created(rows("WHERE i.id=?",id).get(0),token,frontendOrigin.replaceAll("/$","")+"/convite/"+token);
    }
    @Transactional
    public InviteDto cancel(String actor,String id) {
        lock.acquire();var invite=state("id",id,true);
        if(invite==null)throw DomainProblem.missing("Convite não encontrado.");
        if(InvitePolicy.status(invite.used(),invite.cancelled(),invite.expires(),Instant.now())!=InvitePolicy.Status.PENDING)
            throw DomainProblem.conflict("Apenas convites pendentes podem ser cancelados.");
        jdbc.update("UPDATE invite SET cancelled_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",id);
        audit.record(actor,"invite.cancelled","Invite",id);
        return rows("WHERE i.id=?",id).get(0);
    }
    private State state(String key,String value,boolean locked) {
        // key is a private constant at the call sites, never supplied by an API client.
        var found=jdbc.query("SELECT id,email,role_id,expires_at,used_at,cancelled_at FROM invite WHERE "+key+"=?"+(locked?" FOR UPDATE":""),
            (rs,n)->new State(rs.getString("id"),rs.getString("email"),rs.getString("role_id"),instant(rs,"expires_at"),instant(rs,"used_at"),instant(rs,"cancelled_at")),value);
        return found.isEmpty()?null:found.get(0);
    }
    private State valid(String token,boolean locked) {
        if(!InvitePolicy.validFormat(token))throw DomainProblem.invalidInvite();
        var invite=state("token_hash",InvitePolicy.hash(token),locked);
        if(invite==null)throw DomainProblem.invalidInvite();
        InvitePolicy.pending(invite.used(),invite.cancelled(),invite.expires(),Instant.now());
        validateTeams(teamIds(invite.id()),true);
        return invite;
    }
    private List<String> teamIds(String id) { return jdbc.queryForList("SELECT team_id FROM invite_team WHERE invite_id=?",String.class,id); }
    private void validateTeams(Collection<String> ids,boolean publicRequest) {
        for(String id:ids)if(jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE id=? AND archived_at IS NULL",Long.class,id)==0){
            if(publicRequest)throw DomainProblem.invalidInvite();
            throw DomainProblem.conflict("Uma ou mais equipes não estão disponíveis.");
        }
    }
    @Transactional(readOnly=true)
    public PublicInvite inspect(String token) {
        var invite=valid(token,false);var dto=rows("WHERE i.id=?",invite.id()).get(0);
        return new PublicInvite(dto.email(),dto.role().name(),dto.teams().stream().map(UserManagementService.TeamRef::name).toList(),dto.expiresAt());
    }
    @Transactional
    public Accepted accept(String token,String name,String password,String confirmation) {
        InvitePolicy.acceptance(name,password,confirmation);
        lock.acquire();var invite=valid(token,true);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email=?",Long.class,invite.email())>0)throw DomainProblem.invalidInvite();
        String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO app_user (id,name,email,password_hash,created_at,updated_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",id,name.trim(),invite.email(),passwords.encode(password));
        jdbc.update("INSERT INTO app_profile (user_id,status,role_id,created_at,updated_at) VALUES (?,'ACTIVE',?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",id,invite.roleId());
        for(String team:teamIds(invite.id()))jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))",id,team);
        jdbc.update("UPDATE invite SET used_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",invite.id());
        audit.record(id,"invite.accepted","Invite",invite.id());
        return new Accepted(id,invite.id());
    }
}
