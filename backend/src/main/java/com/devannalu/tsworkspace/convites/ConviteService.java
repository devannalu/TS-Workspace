package com.devannalu.tsworkspace.convites;

import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import com.devannalu.tsworkspace.auth.EmailNormalizer;
import com.devannalu.tsworkspace.compartilhado.*;
import com.devannalu.tsworkspace.usuarios.UsuarioService;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import com.devannalu.tsworkspace.convites.ConviteRepository.EstadoConvite;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConviteService {
    public record AutorConvite(String id,String name) { }
    public record ConviteResponse(String id,String email,UsuarioService.PerfilAcesso role,List<UsuarioService.EquipeReferencia> teams,AutorConvite invitedBy,Instant createdAt,Instant expiresAt,PoliticaConvite.SituacaoConvite status) { }
    public record PaginaConvites(List<ConviteResponse> items,long total,int page,int size) { }
    public record ConviteCriadoResponse(ConviteResponse invite,String token,String inviteUrl) { }
    public record ConvitePublicoResponse(String email,String role,List<String> teams,Instant expiresAt) { }
    public record ConviteAceitoResponse(String userId,String inviteId) { }
    private final ConviteRepository convites;
    private final BloqueioOrganizacao bloqueioOrganizacao;
    private final AuditoriaRepository auditoria;
    private final PasswordEncoder passwordEncoder;
    private final String origemFrontend;
    public ConviteService(ConviteRepository convites,BloqueioOrganizacao bloqueioOrganizacao,AuditoriaRepository auditoria,PasswordEncoder passwordEncoder,@Value("${app.frontend-origin}") String origemFrontend) {
        this.convites=convites;this.bloqueioOrganizacao=bloqueioOrganizacao;this.auditoria=auditoria;this.passwordEncoder=passwordEncoder;this.origemFrontend=origemFrontend;
    }


    @Transactional(readOnly=true)
    public PaginaConvites listarConvites(int pagina,int tamanhoPagina) {
        if(pagina<0||pagina>100000||tamanhoPagina<1||tamanhoPagina>100)throw new IllegalArgumentException();
        return new PaginaConvites(convites.buscarConvites("ORDER BY i.created_at DESC,i.id LIMIT ? OFFSET ?",tamanhoPagina,pagina*tamanhoPagina),convites.contarConvites(),pagina,tamanhoPagina);
    }
    @Transactional(readOnly = true)
    public long contarConvitesPendentes() {
        return convites.contarConvitesPendentes();
    }
    @Transactional
    public ConviteCriadoResponse criarConvite(String atorId,String email,String perfilAcessoId,List<String> equipes) {
        return criarConvite(atorId,email,perfilAcessoId,equipes,PoliticaConvite.PRAZO_PADRAO_CONVITE_DIAS);
    }
    @Transactional
    public ConviteCriadoResponse criarConvite(String atorId,String email,String perfilAcessoId,List<String> equipes,int prazoDias) {
        if(prazoDias<1||prazoDias>30)throw new IllegalArgumentException();
        bloqueioOrganizacao.adquirir();email=EmailNormalizer.normalize(email);
        if(convites.contarUsuariosPorEmail(email)>0)throw ProblemaDominio.conflito("Já existe uma usuária com este e-mail.");
        if(convites.contarConvitesPendentesPorEmail(email)>0)throw ProblemaDominio.conflito("Já existe um convite pendente para este e-mail.");
        if(convites.contarPerfilAcesso(perfilAcessoId)==0)throw ProblemaDominio.naoEncontrado("Cargo não encontrado.");
        Set<String> equipesSolicitadas=new LinkedHashSet<>(equipes);
        if(equipesSolicitadas.isEmpty()||equipes.size()>20)throw new IllegalArgumentException();
        validarEquipesConvite(equipesSolicitadas,false);
        String token=PoliticaConvite.gerarToken(),id=UUID.randomUUID().toString();
        Instant agora=Instant.now(),expiraEm=agora.plus(Duration.ofDays(prazoDias));
        convites.inserirConvite(id, email, PoliticaConvite.calcularHashToken(token), expiraEm, atorId, perfilAcessoId, agora);
        for(String equipeId:equipesSolicitadas)convites.inserirEquipeConvite(id, equipeId, agora);
        auditoria.registrar(atorId,"invite.created","Invite",id);
        // O link é devolvido uma única vez ao frontend oficial.
        return new ConviteCriadoResponse(convites.buscarConvites("WHERE i.id=?",id).get(0),token,origemFrontend.replaceAll("/$","")+"/convite/"+token);
    }
    @Transactional
    public ConviteResponse cancelarConvite(String atorId,String id) {
        bloqueioOrganizacao.adquirir();var convite=convites.buscarEstadoConvite("id",id,true);
        if(convite==null)throw ProblemaDominio.naoEncontrado("Convite não encontrado.");
        if(PoliticaConvite.calcularSituacao(convite.utilizadoEm(),convite.canceladoEm(),convite.expiraEm(),Instant.now())!=PoliticaConvite.SituacaoConvite.PENDING)
            throw ProblemaDominio.conflito("Apenas convites pendentes podem ser cancelados.");
        convites.cancelarConvite(id);
        auditoria.registrar(atorId,"invite.cancelled","Invite",id);
        return convites.buscarConvites("WHERE i.id=?",id).get(0);
    }

    private EstadoConvite validarConviteDisponivel(String token,boolean bloquear) {
        if(!PoliticaConvite.possuiFormatoValido(token))throw ProblemaDominio.conviteIndisponivel();
        var convite=convites.buscarEstadoConvite("token_hash",PoliticaConvite.calcularHashToken(token),bloquear);
        if(convite==null)throw ProblemaDominio.conviteIndisponivel();
        PoliticaConvite.exigirConvitePendente(convite.utilizadoEm(),convite.canceladoEm(),convite.expiraEm(),Instant.now());
        validarEquipesConvite(convites.buscarEquipesConvite(convite.id()),true);
        return convite;
    }

    private void validarEquipesConvite(Collection<String> ids,boolean consultaPublica) {
        for(String id:ids)if(convites.contarEquipeAtiva(id)==0){
            if(consultaPublica)throw ProblemaDominio.conviteIndisponivel();
            throw ProblemaDominio.conflito("Uma ou mais equipes não estão disponíveis.");
        }
    }
    @Transactional(readOnly=true)
    public ConvitePublicoResponse consultarConvitePublico(String token) {
        var convite=validarConviteDisponivel(token,false);var dto=convites.buscarConvites("WHERE i.id=?",convite.id()).get(0);
        return new ConvitePublicoResponse(dto.email(),dto.role().name(),dto.teams().stream().map(UsuarioService.EquipeReferencia::name).toList(),dto.expiresAt());
    }
    @Transactional
    public ConviteAceitoResponse aceitarConvite(String token,String nome,String senha,String confirmacaoSenha) {
        PoliticaConvite.validarAceiteConvite(nome,senha,confirmacaoSenha);
        bloqueioOrganizacao.adquirir();var convite=validarConviteDisponivel(token,true);
        if(convites.contarUsuariosPorEmail(convite.email())>0)throw ProblemaDominio.conviteIndisponivel();
        String id=UUID.randomUUID().toString();
        convites.inserirUsuario(id, nome, convite.email(), passwordEncoder.encode(senha));
        convites.inserirPerfil(id, convite.perfilAcessoId());
        for(String equipeId:convites.buscarEquipesConvite(convite.id()))convites.inserirIntegrante(id, equipeId);
        convites.marcarConviteUtilizado(convite.id());
        auditoria.registrar(id,"invite.accepted","Invite",convite.id());
        return new ConviteAceitoResponse(id,convite.id());
    }
}
