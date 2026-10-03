package com.devannalu.tsworkspace.usuarios;

import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import com.devannalu.tsworkspace.autenticacao.RevogacaoSessaoService;
import com.devannalu.tsworkspace.compartilhado.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    public record PerfilAcesso(String id,String key,String name) { }
    public record EquipeReferencia(String id,String name) { }
    public record UsuarioResponse(String id,String name,String email,String jobTitle,String status,PerfilAcesso role,List<EquipeReferencia> teams,Instant createdAt) { }
    public record PaginaUsuarios(List<UsuarioResponse> items,long total,int page,int size) { }
    public record OpcoesUsuarios(List<PerfilAcesso> roles,List<EquipeReferencia> teams) { }
    private final GestaoUsuariosRepository gestaoUsuarios;
    private final BloqueioOrganizacao bloqueioOrganizacao;
    private final AuditoriaRepository auditoria;
    private final RevogacaoSessaoService sessoes;
    public UsuarioService(GestaoUsuariosRepository gestaoUsuarios,BloqueioOrganizacao bloqueioOrganizacao,AuditoriaRepository auditoria,RevogacaoSessaoService sessoes) {
        this.gestaoUsuarios=gestaoUsuarios; this.bloqueioOrganizacao=bloqueioOrganizacao; this.auditoria=auditoria; this.sessoes=sessoes;
    }

    @Transactional(readOnly=true)
    public PaginaUsuarios listarUsuarios(int pagina,int tamanhoPagina,String status,String filtroPerfilAcessoId,String equipe,String busca) {
        if (pagina < 0 || pagina > 100000 || tamanhoPagina < 1 || tamanhoPagina > 100) throw new IllegalArgumentException();
        if (busca != null && !busca.isBlank() && busca.length() > 160) throw new IllegalArgumentException();
        return gestaoUsuarios.listarUsuarios(pagina, tamanhoPagina, status, filtroPerfilAcessoId, equipe, busca);
    }
    @Transactional(readOnly=true)
    public OpcoesUsuarios buscarOpcoesUsuarios() {
        return gestaoUsuarios.buscarOpcoesUsuarios();
    }
    @Transactional(readOnly=true)
    public UsuarioResponse buscarUsuario(String id) {
        var usuariosEncontrados=gestaoUsuarios.buscarUsuariosPorId(id);
        if(usuariosEncontrados.isEmpty())throw ProblemaDominio.naoEncontrado("Usuária ou perfil não encontrado.");
        return usuariosEncontrados.get(0);
    }
    @Transactional
    public UsuarioResponse editarUsuario(String atorId,String id,String cargo,String perfilAcessoId,List<String> equipeIds) {
        bloqueioOrganizacao.adquirir(); var usuarioAnterior=buscarUsuario(id);
        String proximoPerfilAcessoId=perfilAcessoId==null?usuarioAnterior.role().id():perfilAcessoId;
        var perfisAcesso=gestaoUsuarios.buscarPerfilAcesso(proximoPerfilAcessoId);
        if(perfisAcesso.isEmpty())throw ProblemaDominio.naoEncontrado("Cargo não encontrado.");
        if(perfilAcessoId!=null||equipeIds!=null)protegerAcessoAdministrativo(atorId,usuarioAnterior,perfisAcesso.get(0).key(),usuarioAnterior.status().equals("ACTIVE"),equipeIds);
        if(equipeIds!=null){
            Set<String> equipesSolicitadas=new LinkedHashSet<>(equipeIds);
            if(equipesSolicitadas.size()>50)throw new IllegalArgumentException();
            for(String equipe:equipesSolicitadas)if(gestaoUsuarios.contarEquipeAtiva(equipe)==0)throw ProblemaDominio.conflito("Uma ou mais equipes não estão disponíveis.");
            Set<String> equipesAnteriores=new HashSet<>(gestaoUsuarios.listarEquipesUsuario(id));
            if(!equipesAnteriores.equals(equipesSolicitadas)){
                for(String equipe:equipesAnteriores)if(!equipesSolicitadas.contains(equipe))gestaoUsuarios.removerIntegrante(id, equipe);
                for(String equipe:equipesSolicitadas)if(!equipesAnteriores.contains(equipe))gestaoUsuarios.inserirIntegrante(id, equipe);
                auditoria.registrar(atorId,"user.teams_changed","User",id);
            }
        }
        String cargoAtualizado=cargo==null?usuarioAnterior.jobTitle():(cargo.trim().isEmpty()?null:cargo.trim());
        if(!usuarioAnterior.role().id().equals(proximoPerfilAcessoId))auditoria.registrar(atorId,"user.role_changed","User",id);
        if(!usuarioAnterior.role().id().equals(proximoPerfilAcessoId)||!Objects.equals(cargoAtualizado,usuarioAnterior.jobTitle()))
            gestaoUsuarios.atualizarPerfil(proximoPerfilAcessoId, cargoAtualizado, id);
        return buscarUsuario(id);
    }
    @Transactional
    public UsuarioResponse alterarSituacaoUsuario(String atorId,String id,boolean usuarioAtivo) {
        bloqueioOrganizacao.adquirir();var usuarioAnterior=buscarUsuario(id);
        protegerAcessoAdministrativo(atorId,usuarioAnterior,usuarioAnterior.role().key(),usuarioAtivo,null);
        String proximaSituacao=usuarioAtivo?"ACTIVE":"INACTIVE";
        if(!usuarioAnterior.status().equals(proximaSituacao)){
            gestaoUsuarios.atualizarSituacao(proximaSituacao, id);
            auditoria.registrar(atorId,"user.status_changed","User",id);
        }
        if(!usuarioAtivo)sessoes.revogarSessoesDoUsuario(id);
        return buscarUsuario(id);
    }
    private void protegerAcessoAdministrativo(String atorId,UsuarioResponse usuarioAnterior,String proximoPerfilAcessoId,boolean usuarioPermaneceraAtivo,List<String> proximasEquipes) {
        long superAdminsAtivas=gestaoUsuarios.contarSuperAdminsAtivas();
        PoliticaUsuario.protegerAcessoAdministrativo(atorId,usuarioAnterior.id(),usuarioAnterior.role().key(),usuarioAnterior.status().equals("ACTIVE"),proximoPerfilAcessoId,usuarioPermaneceraAtivo,superAdminsAtivas);
        String equipeFundadorasId=gestaoUsuarios.buscarEquipeFundadorasId();
        boolean integranteFundadoras=gestaoUsuarios.contarIntegranteFundadoras(usuarioAnterior.id(), equipeFundadorasId)>0;
        long outrasSuperAdminsAtivas=gestaoUsuarios.contarOutrasSuperAdminsFundadoras(equipeFundadorasId, usuarioAnterior.id());
        PoliticaUsuario.protegerFundadoras(integranteFundadoras&&usuarioAnterior.status().equals("ACTIVE")&&usuarioAnterior.role().key().equals("SUPER_ADMIN"),integranteFundadoras&&usuarioPermaneceraAtivo&&proximoPerfilAcessoId.equals("SUPER_ADMIN")&&(proximasEquipes==null||proximasEquipes.contains(equipeFundadorasId)),outrasSuperAdminsAtivas);
    }
}
