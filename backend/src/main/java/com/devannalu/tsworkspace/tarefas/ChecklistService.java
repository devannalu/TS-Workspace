package com.devannalu.tsworkspace.tarefas;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistService {
    public static final String CONFLITO = "O checklist foi atualizado. Atualize os itens e tente novamente.";
    private final ChecklistRepository checklist;
    private final TarefaService tarefas;
    public ChecklistService(ChecklistRepository checklist,TarefaService tarefas) { this.checklist=checklist;this.tarefas=tarefas; }
    public record Lista(List<ChecklistRepository.Item> items,long concluidos,long total,boolean podeEditar) { }
    public record Posicao(String id,long versao) { }
    private TarefaService.TarefaResponse exigirAcesso(String pessoa,String tarefa,boolean escrita) {
        if(escrita) checklist.bloquearTarefa(tarefa);
        var fonte=tarefas.buscarTarefa(pessoa,tarefa);
        if(escrita&&!fonte.capacidades().editar()) throw new AccessDeniedException("Acesso negado.");
        return fonte;
    }
    private Lista resposta(String tarefa,boolean podeEditar) {
        var itens=checklist.listar(tarefa);
        return new Lista(itens,itens.stream().filter(ChecklistRepository.Item::concluido).count(),itens.size(),podeEditar);
    }
    @Transactional(readOnly=true)
    public Lista listar(String pessoa,String tarefa) {
        var fonte=exigirAcesso(pessoa,tarefa,false);return resposta(tarefa,fonte.capacidades().editar());
    }
    private String texto(String valor) {
        if(valor==null||valor.isBlank()||valor.trim().length()>500) throw new IllegalArgumentException();
        return valor.trim();
    }
    @Transactional
    public Lista criar(String pessoa,String tarefa,String texto) {
        exigirAcesso(pessoa,tarefa,true);var itens=checklist.listar(tarefa);
        if(itens.size()>=200) throw ProblemaDominio.conflito("Esta tarefa atingiu o limite de 200 itens de checklist.");
        int ordem=itens.stream().mapToInt(ChecklistRepository.Item::ordem).max().orElse(-1)+1;
        checklist.inserir(UUID.randomUUID().toString(),tarefa,texto(texto),pessoa,ordem);return resposta(tarefa,true);
    }
    private ChecklistRepository.Item item(String tarefa,String id,long versao) {
        var atual=checklist.listar(tarefa).stream().filter(i->i.id().equals(id)).findFirst()
            .orElseThrow(()->ProblemaDominio.naoEncontrado("Item de checklist não encontrado."));
        if(versao<0)throw new IllegalArgumentException();
        if(atual.versao()!=versao)throw ProblemaDominio.conflito(CONFLITO);
        return atual;
    }
    @Transactional
    public Lista editar(String pessoa,String tarefa,String id,String texto,boolean concluido,long versao) {
        exigirAcesso(pessoa,tarefa,true);item(tarefa,id,versao);checklist.editar(id,texto(texto),concluido);return resposta(tarefa,true);
    }
    @Transactional
    public Lista remover(String pessoa,String tarefa,String id,long versao) {
        exigirAcesso(pessoa,tarefa,true);item(tarefa,id,versao);checklist.remover(id);return resposta(tarefa,true);
    }
    @Transactional
    public Lista ordenar(String pessoa,String tarefa,List<Posicao> ordem) {
        exigirAcesso(pessoa,tarefa,true);var atuais=checklist.listar(tarefa);
        if(ordem==null||ordem.size()>200||ordem.stream().anyMatch(p->p==null||p.id()==null||p.versao()<0)) throw new IllegalArgumentException();
        var ids=ordem.stream().map(Posicao::id).toList();
        if(ids.stream().distinct().count()!=ids.size())throw new IllegalArgumentException();
        if(atuais.size()!=ids.size()||!ids.containsAll(atuais.stream().map(ChecklistRepository.Item::id).toList()))throw ProblemaDominio.conflito(CONFLITO);
        for(var posicao:ordem)item(tarefa,posicao.id(),posicao.versao());
        for(int i=0;i<ids.size();i++) { String id=ids.get(i);
            if(atuais.stream().filter(a->a.id().equals(id)).findFirst().orElseThrow().ordem()!=i)checklist.ordenar(id,i);
        }
        return resposta(tarefa,true);
    }
}
