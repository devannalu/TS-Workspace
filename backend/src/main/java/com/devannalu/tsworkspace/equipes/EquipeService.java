package com.devannalu.tsworkspace.equipes;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipeService {
    private final EquipeRepository equipes;
    private final com.devannalu.tsworkspace.compartilhado.BloqueioOrganizacao bloqueioOrganizacao;
    public EquipeService(EquipeRepository equipes, com.devannalu.tsworkspace.compartilhado.BloqueioOrganizacao bloqueioOrganizacao) { this.equipes = equipes; this.bloqueioOrganizacao = bloqueioOrganizacao; }
    public record ResumoEquipe(String id, String key, String name, String description, String parentId, boolean archived, long memberCount) { }
    public record IntegranteEquipe(String id, String name, String email) { }
    public record DetalheEquipe(ResumoEquipe team, List<IntegranteEquipe> members) { }

    @Transactional(readOnly = true)
    public List<ResumoEquipe> listarEquipes() {
        return equipes.listarEquipesAtivas();
    }
    @Transactional(readOnly = true)
    public List<ResumoEquipe> listarEquipesUsuario(String usuarioId) {
        return equipes.listarEquipesUsuario(usuarioId);
    }
    @Transactional(readOnly = true)
    public DetalheEquipe buscarDetalheEquipe(String id) {
        var equipesEncontradas = equipes.buscarResumoEquipe(id);
        if (equipesEncontradas.isEmpty()) throw ProblemaDominio.naoEncontrado("Equipe não encontrada.");
        var integrantes = equipes.listarIntegrantesAtivos(id);
        return new DetalheEquipe(equipesEncontradas.get(0), integrantes);
    }
    // O mesmo bloqueio protege a árvore e a última administradora entre instâncias.
    private void bloquearHierarquia() {
        bloqueioOrganizacao.adquirir();
    }
    private List<PoliticaEquipe.NoHierarquia> listarHierarquia() {
        return equipes.listarHierarquia();
    }
    private PoliticaEquipe.NoHierarquia buscarEquipe(String id) {
        return listarHierarquia().stream().filter(equipe -> equipe.id().equals(id)).findFirst().orElseThrow(() -> ProblemaDominio.naoEncontrado("Equipe não encontrada."));
    }
    private PoliticaEquipe.NoHierarquia buscarEquipeAtiva(String id) {
        var equipe = buscarEquipe(id);
        if (equipe.archived()) throw ProblemaDominio.conflito("Equipe indisponível.");
        return equipe;
    }
    @Transactional
    public DetalheEquipe criarEquipe(String nome, String descricao, String equipeMaeId) {
        bloquearHierarquia();
        String id = UUID.randomUUID().toString();
        PoliticaEquipe.validarEquipeMae(listarHierarquia(), id, equipeMaeId);
        equipes.inserirEquipe(id, nome, descricao, equipeMaeId);
        return buscarDetalheEquipe(id);
    }
    @Transactional
    public DetalheEquipe editarEquipe(String id, String nome, String descricao, String equipeMaeId) {
        bloquearHierarquia(); buscarEquipe(id);
        PoliticaEquipe.validarEquipeMae(listarHierarquia(), id, equipeMaeId);
        equipes.atualizarEquipe(id, nome, descricao, equipeMaeId);
        return buscarDetalheEquipe(id);
    }
    @Transactional
    public DetalheEquipe arquivarEquipe(String id) {
        bloquearHierarquia(); var equipe = buscarEquipe(id);
        long subequipesAtivas = equipes.contarSubequipesAtivas(id);
        PoliticaEquipe.arquivarEquipe(equipe, subequipesAtivas);
        if (!equipe.archived()) equipes.arquivarEquipe(id);
        return buscarDetalheEquipe(id);
    }
    @Transactional
    public void adicionarIntegrante(String equipeId, String usuarioId) {
        bloquearHierarquia(); buscarEquipeAtiva(equipeId);
        var perfis = equipes.buscarStatusUsuarioBloqueado(usuarioId);
        if (perfis.isEmpty()) throw ProblemaDominio.naoEncontrado("Usuária não encontrada.");
        boolean integranteDuplicada = equipes.contarIntegrante(equipeId, usuarioId) > 0;
        PoliticaEquipe.adicionarIntegrante("ACTIVE".equals(perfis.get(0).get("status")), integranteDuplicada);
        equipes.inserirIntegrante(usuarioId, equipeId);
    }
    @Transactional
    public void removerIntegrante(String equipeId, String usuarioId) {
        bloquearHierarquia(); var equipe = buscarEquipeAtiva(equipeId);
        var perfis = equipes.buscarPerfilAcessoUsuarioBloqueado(usuarioId);
        if (perfis.isEmpty()) throw ProblemaDominio.naoEncontrado("Usuária não encontrada.");
        if (equipes.contarIntegrante(equipeId, usuarioId) == 0)
            throw ProblemaDominio.naoEncontrado("A integrante não está nesta equipe.");
        long superAdminsAtivas = equipes.contarSuperAdminsAtivas(equipeId);
        PoliticaEquipe.removerIntegrante(equipe.key(), (String) perfis.get(0).get("role_key"), superAdminsAtivas);
        equipes.removerIntegrante(usuarioId, equipeId);
    }
}
