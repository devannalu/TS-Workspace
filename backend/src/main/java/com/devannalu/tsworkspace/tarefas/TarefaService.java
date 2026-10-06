package com.devannalu.tsworkspace.tarefas;

import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import com.devannalu.tsworkspace.compartilhado.BloqueioOrganizacao;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.tarefas.PoliticaTarefa.Acesso;
import com.devannalu.tsworkspace.tarefas.TarefaRepository.EstadoTarefa;
import com.devannalu.tsworkspace.tarefas.TarefaRepository.Referencia;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarefaService {
    private final TarefaRepository tarefas;
    private final PermissaoService permissoes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    private final com.devannalu.tsworkspace.projetos.ProjetoRepository projetos;
    public TarefaService(TarefaRepository tarefas, PermissaoService permissoes, BloqueioOrganizacao bloqueio,
        AuditoriaRepository auditoria, com.devannalu.tsworkspace.projetos.ProjetoRepository projetos) {
        this.tarefas = tarefas; this.permissoes = permissoes; this.bloqueio = bloqueio; this.auditoria = auditoria;
        this.projetos = projetos;
    }

    public record Capacidades(boolean editar, boolean atribuir, boolean arquivar) { }
    public record TarefaResponse(String id, String titulo, String descricao, Tarefa.Status status,
        Tarefa.Prioridade prioridade, LocalDate prazo, int ordem, long versao, Referencia equipe, Referencia criadaPor,
        List<Referencia> responsaveis, Instant criadaEm, Instant atualizadaEm, boolean arquivada,
        boolean atrasada, Capacidades capacidades, Referencia projeto) { }
    public record PaginaTarefas(List<TarefaResponse> items, long total, int page, int size) { }
    public record FiltrosTarefas(String equipeId, Tarefa.Status status, Tarefa.Prioridade prioridade,
        String responsavelId, String busca, LocalDate prazoDe, LocalDate prazoAte, boolean arquivadas, int pagina, int tamanho,
        String projetoId) {
        public FiltrosTarefas(String equipeId, Tarefa.Status status, Tarefa.Prioridade prioridade, String responsavelId,
            String busca, LocalDate prazoDe, LocalDate prazoAte, boolean arquivadas, int pagina, int tamanho) {
            this(equipeId,status,prioridade,responsavelId,busca,prazoDe,prazoAte,arquivadas,pagina,tamanho,null);
        }
    }
    public record ResumoTarefas(long minhasTarefas, long emAndamento, long vencendoHoje, long atrasadas) { }
    public record OpcoesTarefas(List<Referencia> equipes, List<Referencia> responsaveis) { }

    private LocalDate hoje() { return LocalDate.now(ZoneId.of("America/Bahia")); }

    private Acesso exigirAcesso(String usuarioId, String permissao) {
        var atuais = permissoes.buscarPermissoesUsuario(usuarioId);
        Set<String> chaves = Set.copyOf(atuais.chavesEfetivas());
        if (atuais.role() == null || !chaves.contains(permissao)) throw new AccessDeniedException("Acesso negado.");
        return new Acesso(usuarioId, atuais.role().key(), chaves);
    }

    private void exigirEquipe(Acesso acesso, String equipeId, boolean ativa) {
        if (equipeId == null) throw new IllegalArgumentException();
        boolean arquivada = tarefas.equipeArquivada(equipeId)
            .orElseThrow(() -> ProblemaDominio.naoEncontrado("Equipe não encontrada."));
        PoliticaTarefa.exigirEquipe(acesso, tarefas.integrante(equipeId, acesso.usuarioId()));
        if (ativa) PoliticaTarefa.exigirEquipeAtiva(arquivada);
    }

    private EstadoTarefa buscarNoEscopo(Acesso acesso, String id) {
        var tarefa = tarefas.buscar(id).orElseThrow(() -> ProblemaDominio.naoEncontrado("Tarefa não encontrada."));
        PoliticaTarefa.exigirEquipe(acesso, tarefas.integrante(tarefa.equipe().id(), acesso.usuarioId()));
        return tarefa;
    }

    private TarefaResponse montarResposta(Acesso acesso, EstadoTarefa tarefa, List<Referencia> responsaveis) {
        boolean responsavel = responsaveis.stream().anyMatch(pessoa -> pessoa.id().equals(acesso.usuarioId()));
        boolean alteravel = tarefa.arquivadaEm() == null && !tarefa.equipeArquivada() && !tarefa.projetoArquivado();
        boolean editar = alteravel && PoliticaTarefa.podeEditar(acesso, tarefa.criadaPor().id(), responsavel);
        return new TarefaResponse(tarefa.id(), tarefa.titulo(), tarefa.descricao(), tarefa.status(), tarefa.prioridade(),
            tarefa.prazo(), tarefa.ordem(), tarefa.versao(), tarefa.equipe(), tarefa.criadaPor(), responsaveis,
            tarefa.criadaEm(), tarefa.atualizadaEm(), tarefa.arquivadaEm() != null,
            PoliticaTarefa.atrasada(tarefa.prazo(), tarefa.status(), hoje()),
            new Capacidades(editar, editar && acesso.permissoes().contains("tasks.assign"),
                tarefa.arquivadaEm() == null && !tarefa.projetoArquivado() && acesso.permissoes().contains("tasks.archive")), tarefa.projeto());
    }

    private List<Referencia> responsaveis(String id) {
        return tarefas.buscarResponsaveis(List.of(id)).getOrDefault(id, List.of());
    }

    private Set<String> validarResponsaveis(Acesso acesso, String equipeId, List<String> ids, Set<String> anteriores) {
        if (ids != null && ids.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException();
        Set<String> novos = ids == null ? anteriores : new LinkedHashSet<>(ids);
        if (novos.size() > 50) throw new IllegalArgumentException();
        if (!novos.equals(anteriores) && !acesso.permissoes().contains("tasks.assign")) throw new AccessDeniedException("Acesso negado.");
        if (tarefas.contarResponsaveisElegiveis(equipeId, novos) != novos.size()) throw new IllegalArgumentException();
        return novos;
    }

    @Transactional(readOnly = true)
    public PaginaTarefas listarTarefas(String usuarioId, FiltrosTarefas filtros) {
        Acesso acesso = exigirAcesso(usuarioId, "tasks.view");
        if (filtros.pagina() < 0 || filtros.pagina() > 100000 || filtros.tamanho() < 1 || filtros.tamanho() > 100
            || (filtros.busca() != null && filtros.busca().length() > 200)
            || (filtros.prazoDe() != null && filtros.prazoAte() != null && filtros.prazoDe().isAfter(filtros.prazoAte())))
            throw new IllegalArgumentException();
        if (filtros.equipeId() != null) exigirEquipe(acesso, filtros.equipeId(), false);
        var consulta = tarefas.montarConsulta(acesso, filtros);
        var encontradas = tarefas.listar(consulta, filtros.pagina(), filtros.tamanho());
        var responsaveis = tarefas.buscarResponsaveis(encontradas.stream().map(EstadoTarefa::id).toList());
        return new PaginaTarefas(encontradas.stream().map(tarefa -> montarResposta(acesso, tarefa,
            responsaveis.getOrDefault(tarefa.id(), List.of()))).toList(), tarefas.contar(consulta), filtros.pagina(), filtros.tamanho());
    }

    @Transactional(readOnly = true)
    public TarefaResponse buscarTarefa(String usuarioId, String id) {
        var acesso = exigirAcesso(usuarioId, "tasks.view");
        return montarResposta(acesso, buscarNoEscopo(acesso, id), responsaveis(id));
    }

    @Transactional(readOnly = true)
    public OpcoesTarefas buscarOpcoes(String usuarioId, String equipeId) {
        var acesso = exigirAcesso(usuarioId, "tasks.view");
        if (equipeId != null) exigirEquipe(acesso, equipeId, true);
        return new OpcoesTarefas(tarefas.listarEquipesDisponiveis(acesso), equipeId == null ? List.of()
            : tarefas.listarResponsaveisElegiveis(equipeId));
    }

    @Transactional(readOnly = true)
    public ResumoTarefas resumirTarefas(String usuarioId) {
        return tarefas.resumir(exigirAcesso(usuarioId, "tasks.view"), hoje());
    }

    @Transactional
    public TarefaResponse criarTarefa(String usuarioId, String titulo, String descricao, Tarefa.Prioridade prioridade,
        String equipeId, LocalDate prazo, List<String> responsavelIds) {
        return criarTarefa(usuarioId,titulo,descricao,prioridade,equipeId,prazo,responsavelIds,null);
    }

    @Transactional
    public TarefaResponse criarTarefa(String usuarioId, String titulo, String descricao, Tarefa.Prioridade prioridade,
        String equipeId, LocalDate prazo, List<String> responsavelIds, String projetoId) {
        bloqueio.adquirir();
        var acesso = exigirAcesso(usuarioId, "tasks.create");
        exigirEquipe(acesso, equipeId, true);
        validarVinculoProjeto(acesso, projetoId, equipeId, true);
        var novos = validarResponsaveis(acesso, equipeId, responsavelIds, Set.of());
        String id = UUID.randomUUID().toString();
        tarefas.inserir(id, PoliticaTarefa.validarTitulo(titulo), PoliticaTarefa.validarDescricao(descricao),
            PoliticaTarefa.prioridadeInicial(prioridade), equipeId, usuarioId, prazo, projetoId);
        if (projetoId != null) projetos.registrarPrimeiroVinculo(projetoId);
        tarefas.substituirResponsaveis(id, novos);
        auditoria.registrar(usuarioId, "task.created", "Task", id);
        if (!novos.isEmpty()) auditoria.registrar(usuarioId, "task.assignees_changed", "Task", id);
        return montarResposta(acesso, buscarNoEscopo(acesso, id), responsaveis(id));
    }

    private EstadoTarefa exigirEdicao(Acesso acesso, String id, long versao) {
        var tarefa = buscarNoEscopo(acesso, id);
        PoliticaTarefa.exigirVersao(tarefa.versao(), versao);
        if (tarefa.arquivadaEm() != null) throw ProblemaDominio.conflito("Tarefa arquivada: somente leitura.");
        PoliticaTarefa.exigirEquipeAtiva(tarefa.equipeArquivada());
        boolean responsavel = responsaveis(id).stream().anyMatch(pessoa -> pessoa.id().equals(acesso.usuarioId()));
        if (!PoliticaTarefa.podeEditar(acesso, tarefa.criadaPor().id(), responsavel)) throw new AccessDeniedException("Acesso negado.");
        return tarefa;
    }

    @Transactional
    public TarefaResponse editarTarefa(String usuarioId, String id, String titulo, String descricao,
        Tarefa.Prioridade prioridade, String equipeId, LocalDate prazo, List<String> responsavelIds, long versao) {
        return editarTarefa(usuarioId,id,titulo,descricao,prioridade,equipeId,prazo,responsavelIds,versao,null);
    }

    @Transactional
    public TarefaResponse editarTarefa(String usuarioId, String id, String titulo, String descricao,
        Tarefa.Prioridade prioridade, String equipeId, LocalDate prazo, List<String> responsavelIds, long versao, String projetoId) {
        bloqueio.adquirir();
        var acesso = exigirAcesso(usuarioId, "tasks.edit");
        var atual = exigirEdicao(acesso, id, versao);
        String anterior = atual.projeto() == null ? null : atual.projeto().id();
        if (anterior != null) validarProjetoAlteravel(acesso, anterior);
        validarVinculoProjeto(acesso, projetoId, equipeId, !Objects.equals(anterior, projetoId));
        exigirEquipe(acesso, equipeId, true);
        var anteriores = new HashSet<>(responsaveis(id).stream().map(Referencia::id).toList());
        var novos = validarResponsaveis(acesso, equipeId, responsavelIds, anteriores);
        if (tarefas.atualizar(atual, PoliticaTarefa.validarTitulo(titulo), PoliticaTarefa.validarDescricao(descricao),
            PoliticaTarefa.prioridadeInicial(prioridade), equipeId, prazo, projetoId) != 1) PoliticaTarefa.exigirVersao(-1, versao);
        if (projetoId != null) projetos.registrarPrimeiroVinculo(projetoId);
        if (!novos.equals(anteriores)) {
            tarefas.substituirResponsaveis(id, novos);
            auditoria.registrar(usuarioId, "task.assignees_changed", "Task", id);
        }
        auditoria.registrar(usuarioId, "task.updated", "Task", id);
        return montarResposta(acesso, buscarNoEscopo(acesso, id), responsaveis(id));
    }

    @Transactional
    public TarefaResponse moverTarefa(String usuarioId, String id, Tarefa.Status status, String antesDeId, long versao) {
        bloqueio.adquirir();
        var acesso = exigirAcesso(usuarioId, "tasks.edit");
        var atual = exigirEdicao(acesso, id, versao);
        if (status == null || id.equals(antesDeId)) throw new IllegalArgumentException();
        if (atual.projeto() != null) {
            var projeto = validarProjetoAlteravel(acesso, atual.projeto().id());
            if (atual.status() == Tarefa.Status.CONCLUIDA && status != Tarefa.Status.CONCLUIDA
                && projeto.status() == com.devannalu.tsworkspace.projetos.Projeto.Status.CONCLUIDO)
                throw ProblemaDominio.conflito("Reabra o projeto antes de reabrir esta tarefa.");
        }
        if (antesDeId != null) {
            var destino = buscarNoEscopo(acesso, antesDeId);
            if (destino.status() != status || destino.arquivadaEm() != null) throw new IllegalArgumentException();
        }
        // O bloqueio já compartilhado com equipes impede corridas de ordenação e de vínculos.
        var destino = new ArrayList<>(tarefas.listarColuna(status)); destino.remove(id);
        if (antesDeId == null) destino.add(id); else destino.add(destino.indexOf(antesDeId), id);
        if (tarefas.mudarStatus(atual, status) != 1) PoliticaTarefa.exigirVersao(-1, versao);
        tarefas.renumerarColuna(destino);
        if (atual.status() != status) {
            tarefas.renumerarColuna(tarefas.listarColuna(atual.status()));
            auditoria.registrar(usuarioId, "task.status_changed", "Task", id);
        }
        return montarResposta(acesso, buscarNoEscopo(acesso, id), responsaveis(id));
    }

    @Transactional
    public TarefaResponse arquivarTarefa(String usuarioId, String id, long versao) {
        bloqueio.adquirir();
        var acesso = exigirAcesso(usuarioId, "tasks.archive");
        var atual = buscarNoEscopo(acesso, id);
        if (atual.projeto() != null) validarProjetoAlteravel(acesso, atual.projeto().id());
        PoliticaTarefa.exigirVersao(atual.versao(), versao);
        if (atual.arquivadaEm() == null) {
            if (tarefas.arquivar(atual) != 1) PoliticaTarefa.exigirVersao(-1, versao);
            tarefas.renumerarColuna(tarefas.listarColuna(atual.status()));
            auditoria.registrar(usuarioId, "task.archived", "Task", id);
        }
        return montarResposta(acesso, buscarNoEscopo(acesso, id), responsaveis(id));
    }

    private com.devannalu.tsworkspace.projetos.ProjetoRepository.EstadoProjeto validarProjetoAlteravel(Acesso acesso, String id) {
        var projeto = projetos.buscar(id).orElseThrow(() -> ProblemaDominio.naoEncontrado("Projeto não encontrado."));
        PoliticaTarefa.exigirEquipe(acesso, tarefas.integrante(projeto.equipe().id(), acesso.usuarioId()));
        if (projeto.arquivadaEm() != null) throw ProblemaDominio.conflito("Projeto arquivado: somente leitura.");
        PoliticaTarefa.exigirEquipeAtiva(projeto.equipeArquivada());
        return projeto;
    }

    private void validarVinculoProjeto(Acesso acesso, String id, String equipeId, boolean novo) {
        if (id == null) return;
        if (!acesso.permissoes().contains("projects.view")) throw new AccessDeniedException("Acesso negado.");
        var projeto = validarProjetoAlteravel(acesso, id);
        if (!projeto.equipe().id().equals(equipeId)) throw ProblemaDominio.conflito("A tarefa e o projeto devem pertencer à mesma equipe.");
        if (novo && projeto.status() == com.devannalu.tsworkspace.projetos.Projeto.Status.CONCLUIDO)
            throw ProblemaDominio.conflito("Reabra o projeto antes de vincular novas tarefas.");
    }
}
