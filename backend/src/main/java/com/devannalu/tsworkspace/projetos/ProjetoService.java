package com.devannalu.tsworkspace.projetos;

import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import com.devannalu.tsworkspace.compartilhado.BloqueioOrganizacao;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.tarefas.TarefaRepository;
import com.devannalu.tsworkspace.projetos.PoliticaProjeto.Acesso;
import com.devannalu.tsworkspace.projetos.ProjetoRepository.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjetoService {
    private final ProjetoRepository projetos;
    private final TarefaRepository tarefas;
    private final PermissaoService permissoes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    private final com.devannalu.tsworkspace.notificacoes.NotificacaoService notificacoes;
    public ProjetoService(ProjetoRepository projetos,TarefaRepository tarefas,PermissaoService permissoes,
        BloqueioOrganizacao bloqueio,AuditoriaRepository auditoria,com.devannalu.tsworkspace.notificacoes.NotificacaoService notificacoes) {
        this.projetos=projetos;this.tarefas=tarefas;this.permissoes=permissoes;this.bloqueio=bloqueio;this.auditoria=auditoria;this.notificacoes=notificacoes;
    }
    public record Capacidades(boolean editar,boolean gerenciarResponsaveis,boolean arquivar,boolean trocarEquipe) { }
    public record ProjetoResponse(String id,String titulo,String descricao,Projeto.Status status,Referencia equipe,
        List<Referencia> responsaveis,LocalDate dataInicio,LocalDate dataFim,Referencia criadaPor,
        long totalTarefas,long tarefasConcluidas,Integer percentualProgresso,long emAndamento,long emRevisao,long aFazer,
        long versao,boolean arquivado,Instant criadaEm,Instant atualizadaEm,Capacidades capacidades) { }
    public record PaginaProjetos(List<ProjetoResponse> items,long total,int page,int size) { }
    public record FiltrosProjetos(String equipeId,Projeto.Status status,String responsavelId,String busca,
        LocalDate inicioDe,LocalDate inicioAte,LocalDate fimDe,LocalDate fimAte,boolean arquivados,int pagina,int tamanho) { }
    public record ResumoProjetos(long projetosAtivos,long emAndamento,long comPrazoProximo) { }
    public record OpcoesProjetos(List<Referencia> equipes,List<Referencia> responsaveis) { }
    private Acesso exigirAcesso(String usuarioId,String permissao) {
        var atuais=permissoes.buscarPermissoesUsuario(usuarioId);Set<String> chaves=Set.copyOf(atuais.chavesEfetivas());
        if(atuais.role()==null||!chaves.contains(permissao))throw new AccessDeniedException("Acesso negado.");
        var acesso = new Acesso(usuarioId,atuais.role().key(),chaves);
        if (!permissao.equals("projects.view") && !PoliticaProjeto.podeAlterar(acesso, permissao))
            throw new AccessDeniedException("Acesso negado.");
        return acesso;
    }
    private void exigirEquipe(Acesso acesso,String equipeId,boolean ativa) {
        if(equipeId==null)throw new IllegalArgumentException();
        boolean arquivada=tarefas.equipeArquivada(equipeId).orElseThrow(()->ProblemaDominio.naoEncontrado("Equipe não encontrada."));
        PoliticaProjeto.exigirEquipe(acesso,tarefas.integrante(equipeId,acesso.usuarioId()));
        if(ativa&&arquivada)throw ProblemaDominio.conflito("Equipe arquivada: projetos históricos são somente leitura.");
    }
    private EstadoProjeto buscarNoEscopo(Acesso acesso,String id) {
        var projeto=projetos.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Projeto não encontrado."));
        PoliticaProjeto.exigirEquipe(acesso,tarefas.integrante(projeto.equipe().id(),acesso.usuarioId()));return projeto;
    }
    private List<Referencia> responsaveis(String id) {return projetos.buscarResponsaveis(List.of(id)).getOrDefault(id,List.of());}
    private ProjetoResponse resposta(Acesso acesso,EstadoProjeto p,List<Referencia> responsaveis) {
        boolean alteravel=p.arquivadaEm()==null&&!p.equipeArquivada();
        boolean editar=alteravel&&PoliticaProjeto.podeAlterar(acesso,"projects.edit");
        return new ProjetoResponse(p.id(),p.titulo(),p.descricao(),p.status(),p.equipe(),responsaveis,p.dataInicio(),p.dataFim(),p.criadaPor(),
            p.totalTarefas(),p.tarefasConcluidas(),PoliticaProjeto.percentual(p.totalTarefas(),p.tarefasConcluidas()),p.emAndamento(),p.emRevisao(),p.aFazer(),
            p.versao(),p.arquivadaEm()!=null,p.criadaEm(),p.atualizadaEm(),new Capacidades(editar,
                alteravel&&PoliticaProjeto.podeAlterar(acesso,"projects.manage_members"),alteravel&&PoliticaProjeto.podeAlterar(acesso,"projects.archive"),
                editar&&p.primeiroVinculoTarefaEm()==null));
    }
    private Set<String> validarResponsaveis(Acesso acesso,String equipe,List<String> ids,Set<String> anteriores) {
        if(ids!=null&&ids.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException();
        Set<String> novos=ids==null?anteriores:new LinkedHashSet<>(ids);
        if(novos.size()>50)throw new IllegalArgumentException();
        if(!novos.equals(anteriores)&&!acesso.permissoes().contains("projects.manage_members"))throw new AccessDeniedException("Acesso negado.");
        if(tarefas.contarResponsaveisElegiveis(equipe,novos)!=novos.size())throw new IllegalArgumentException();return novos;
    }
    @Transactional(readOnly=true)
    public PaginaProjetos listarProjetos(String usuarioId,FiltrosProjetos filtros) {
        var acesso=exigirAcesso(usuarioId,"projects.view");
        if(filtros.pagina()<0||filtros.pagina()>100000||filtros.tamanho()<1||filtros.tamanho()>100||filtros.busca()!=null&&filtros.busca().length()>200)
            throw new IllegalArgumentException();
        PoliticaProjeto.validarPeriodo(filtros.inicioDe(),filtros.inicioAte());PoliticaProjeto.validarPeriodo(filtros.fimDe(),filtros.fimAte());
        if(filtros.equipeId()!=null)exigirEquipe(acesso,filtros.equipeId(),false);
        var consulta=projetos.montarConsulta(acesso,filtros);var encontrados=projetos.listar(consulta,filtros.pagina(),filtros.tamanho());
        var responsaveis=projetos.buscarResponsaveis(encontrados.stream().map(EstadoProjeto::id).toList());
        return new PaginaProjetos(encontrados.stream().map(p->resposta(acesso,p,responsaveis.getOrDefault(p.id(),List.of()))).toList(),
            projetos.contar(consulta),filtros.pagina(),filtros.tamanho());
    }
    @Transactional(readOnly=true)
    public ProjetoResponse buscarProjeto(String usuarioId,String id) {
        var acesso=exigirAcesso(usuarioId,"projects.view");return resposta(acesso,buscarNoEscopo(acesso,id),responsaveis(id));
    }
    @Transactional(readOnly=true)
    public OpcoesProjetos buscarOpcoes(String usuarioId,String equipeId) {
        var acesso=exigirAcesso(usuarioId,"projects.view");if(equipeId!=null)exigirEquipe(acesso,equipeId,true);
        var acessoEquipes=new com.devannalu.tsworkspace.tarefas.PoliticaTarefa.Acesso(usuarioId,acesso.perfil(),acesso.permissoes());
        return new OpcoesProjetos(tarefas.listarEquipesDisponiveis(acessoEquipes).stream().map(e->new Referencia(e.id(),e.nome())).toList(),
            equipeId==null?List.of():tarefas.listarResponsaveisElegiveis(equipeId).stream().map(e->new Referencia(e.id(),e.nome())).toList());
    }
    @Transactional(readOnly=true)
    public ResumoProjetos resumirProjetos(String usuarioId) {
        return projetos.resumir(exigirAcesso(usuarioId,"projects.view"),LocalDate.now(ZoneId.of("America/Bahia")));
    }
    @Transactional
    public ProjetoResponse criarProjeto(String usuarioId,String titulo,String descricao,String equipeId,List<String> ids,LocalDate inicio,LocalDate fim) {
        bloqueio.adquirir();var acesso=exigirAcesso(usuarioId,"projects.create");exigirEquipe(acesso,equipeId,true);
        PoliticaProjeto.validarPeriodo(inicio,fim);var novos=validarResponsaveis(acesso,equipeId,ids,Set.of());String id=UUID.randomUUID().toString();
        projetos.inserir(id,PoliticaProjeto.validarTitulo(titulo),PoliticaProjeto.validarDescricao(descricao),equipeId,usuarioId,inicio,fim);
        projetos.substituirResponsaveis(id,novos);auditoria.registrar(usuarioId,"project.created","Project",id);
        notificacoes.avisar(usuarioId,"PROJETO",id,"ATRIBUICAO",novos);
        return resposta(acesso,buscarNoEscopo(acesso,id),responsaveis(id));
    }
    private EstadoProjeto exigirAlteravel(Acesso acesso,String id,long versao) {
        var atual=buscarNoEscopo(acesso,id);PoliticaProjeto.exigirVersao(atual.versao(),versao);
        if(atual.arquivadaEm()!=null)throw ProblemaDominio.conflito("Projeto arquivado: somente leitura.");
        exigirEquipe(acesso,atual.equipe().id(),true);return atual;
    }
    @Transactional
    public ProjetoResponse editarProjeto(String usuarioId,String id,String titulo,String descricao,Projeto.Status status,String equipeId,
        List<String> ids,LocalDate inicio,LocalDate fim,long versao) {
        bloqueio.adquirir();var acesso=exigirAcesso(usuarioId,"projects.edit");var atual=exigirAlteravel(acesso,id,versao);
        exigirEquipe(acesso,equipeId,true);PoliticaProjeto.validarPeriodo(inicio,fim);if(status==null)throw new IllegalArgumentException();
        if(!atual.equipe().id().equals(equipeId)&&atual.primeiroVinculoTarefaEm()!=null)
            throw ProblemaDominio.conflito("A equipe não pode ser alterada porque este projeto já teve tarefas vinculadas.");
        if(status==Projeto.Status.CONCLUIDO)PoliticaProjeto.exigirSemPendencias(atual.totalTarefas(),atual.tarefasConcluidas(),false);
        Set<String> anteriores=new HashSet<>(responsaveis(id).stream().map(Referencia::id).toList());
        var novos=validarResponsaveis(acesso,equipeId,ids,anteriores);
        if(projetos.atualizar(atual,PoliticaProjeto.validarTitulo(titulo),PoliticaProjeto.validarDescricao(descricao),status,equipeId,inicio,fim)!=1)
            PoliticaProjeto.exigirVersao(-1,versao);
        if(!novos.equals(anteriores)){projetos.substituirResponsaveis(id,novos);auditoria.registrar(usuarioId,"project.responsibles_changed","Project",id);}
        var adicionadas=new HashSet<>(novos);adicionadas.removeAll(anteriores);
        notificacoes.avisar(usuarioId,"PROJETO",id,"ATRIBUICAO",adicionadas);
        if(status!=atual.status()||!Objects.equals(atual.dataFim(),fim)) notificacoes.avisarResponsaveis(usuarioId,"PROJETO",id,"ALTERACAO");
        if(status!=atual.status())auditoria.registrar(usuarioId,"project.status_changed","Project",id);
        if(!Objects.equals(atual.titulo(),titulo.trim())||!Objects.equals(atual.descricao(),PoliticaProjeto.validarDescricao(descricao))
            ||!atual.equipe().id().equals(equipeId)||!Objects.equals(atual.dataInicio(),inicio)||!Objects.equals(atual.dataFim(),fim))
            auditoria.registrar(usuarioId,"project.updated","Project",id);
        return resposta(acesso,buscarNoEscopo(acesso,id),responsaveis(id));
    }
    @Transactional
    public ProjetoResponse arquivarProjeto(String usuarioId,String id,long versao) {
        bloqueio.adquirir();var acesso=exigirAcesso(usuarioId,"projects.archive");var atual=exigirAlteravel(acesso,id,versao);
        PoliticaProjeto.exigirSemPendencias(atual.totalTarefas(),atual.tarefasConcluidas(),true);
        if(projetos.arquivar(atual)!=1)PoliticaProjeto.exigirVersao(-1,versao);auditoria.registrar(usuarioId,"project.archived","Project",id);
        return resposta(acesso,buscarNoEscopo(acesso,id),responsaveis(id));
    }
}
