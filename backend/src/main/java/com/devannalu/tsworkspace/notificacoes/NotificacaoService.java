package com.devannalu.tsworkspace.notificacoes;

import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.tarefas.PoliticaTarefa;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoService {
    private final NotificacaoRepository notificacoes;
    private final PermissaoService permissoes;
    public NotificacaoService(NotificacaoRepository notificacoes,PermissaoService permissoes) { this.notificacoes=notificacoes;this.permissoes=permissoes; }
    public record Pagina(List<NotificacaoRepository.Item> items,long total,long naoLidas,int page,int size) { }
    @Transactional
    public void avisar(String autora,String tipo,String id,String motivo,Collection<String> destinatarias) {
        String chave=motivo+":"+UUID.randomUUID();
        for(String pessoa:new HashSet<>(destinatarias)) if(!pessoa.equals(autora)) notificacoes.inserir(pessoa,tipo,id,motivo,chave);
    }
    @Transactional
    public void avisarResponsaveis(String autora,String tipo,String id,String motivo) {
        avisar(autora,tipo,id,motivo,notificacoes.responsaveis(tipo,id));
    }
    @Transactional
    public Pagina listar(String pessoa,int pagina,int tamanho) {
        if(pagina<0||pagina>100000||tamanho<1||tamanho>100)throw new IllegalArgumentException();
        var acesso=permissoes.buscarPermissoesUsuario(pessoa);
        Set<String> chaves=Set.copyOf(acesso.chavesEfetivas());
        boolean global=acesso.role()!=null&&new PoliticaTarefa.Acesso(pessoa,acesso.role().key(),chaves).global();
        var consulta=notificacoes.consulta(pessoa,global,chaves.contains("tasks.view"),chaves.contains("projects.view"));
        LocalDate hoje=LocalDate.now(ZoneId.of("America/Bahia"));
        for(var prazo:notificacoes.prazos(pessoa,hoje.plusDays(2))) {
            String motivo=prazo.data().isBefore(hoje)?"ATRASO":"PRAZO";
            notificacoes.inserir(pessoa,prazo.tipo(),prazo.id(),motivo,motivo+":"+prazo.tipo()+":"+prazo.id()+":"+prazo.data());
        }
        return new Pagina(notificacoes.listar(consulta,pagina,tamanho),notificacoes.contar(consulta,false),notificacoes.contar(consulta,true),pagina,tamanho);
    }
    @Transactional
    public void marcarLida(String pessoa,String id) {
        if(id!=null&&!notificacoes.pertence(id,pessoa))throw ProblemaDominio.naoEncontrado("Notificação não encontrada.");
        notificacoes.ler(pessoa,id);
    }
}
