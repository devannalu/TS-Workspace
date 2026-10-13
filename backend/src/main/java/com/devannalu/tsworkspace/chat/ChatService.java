package com.devannalu.tsworkspace.chat;

import com.devannalu.tsworkspace.chat.ChatRepository.*;
import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.compartilhado.*;
import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ChatRepository chat;
    private final PermissaoService permissoes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    public ChatService(ChatRepository chat,PermissaoService permissoes,BloqueioOrganizacao bloqueio,AuditoriaRepository auditoria) {
        this.chat=chat;this.permissoes=permissoes;this.bloqueio=bloqueio;this.auditoria=auditoria;
    }
    private record Acesso(Set<String> permissoes,boolean global) { }
    public record Capacidades(boolean editar,boolean remover) { }
    public record Resposta(String id,Pessoa autora,String texto,long versao,Instant criadaEm,Instant atualizadaEm,boolean removida,Capacidades capacidades) { }
    public record Pagina(List<Resposta> items,String proximaPagina,boolean podeEnviar,Canal canal) { }
    private Acesso acesso(String pessoa) {
        var atuais=permissoes.buscarPermissoesUsuario(pessoa);var chaves=Set.copyOf(atuais.chavesEfetivas());
        if(atuais.role()==null||!chaves.contains("chat.view"))throw new AccessDeniedException("Acesso negado.");
        return new Acesso(chaves,Set.of("SUPER_ADMIN","ADMIN").contains(atuais.role().key()));
    }
    private Canal canal(String pessoa,Acesso acesso,String id) {
        if("geral".equals(id))return new Canal("geral","Geral do Workspace",false);
        UUID.fromString(id);
        var canal=chat.canal(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Canal não encontrado."));
        if(!acesso.global()&&!chat.integrante(pessoa,id))throw new AccessDeniedException("Acesso negado.");
        return canal;
    }
    private String equipe(Canal canal) {return "geral".equals(canal.id())?null:canal.id();}
    private String canal(Mensagem mensagem) {return mensagem.equipeId()==null?"geral":mensagem.equipeId();}
    private boolean enviar(Acesso acesso,Canal canal) {return !canal.arquivado()&&acesso.permissoes().contains("chat.send");}
    private Resposta resposta(String pessoa,Acesso acesso,Canal canal,Mensagem mensagem) {
        boolean editar=!mensagem.removida()&&enviar(acesso,canal)&&mensagem.autora().id().equals(pessoa);
        boolean remover=!mensagem.removida()&&!canal.arquivado()&&(editar||acesso.permissoes().contains("chat.moderate"));
        return new Resposta(mensagem.id(),mensagem.autora(),mensagem.removida()?null:mensagem.texto(),mensagem.versao(),mensagem.criadaEm(),mensagem.atualizadaEm(),mensagem.removida(),new Capacidades(editar,remover));
    }
    @Transactional(readOnly=true)
    public List<Canal> canais(String pessoa) {
        var acesso=acesso(pessoa);var canais=new ArrayList<Canal>();canais.add(new Canal("geral","Geral do Workspace",false));canais.addAll(chat.canais(pessoa,acesso.global()));return List.copyOf(canais);
    }
    @Transactional(readOnly=true)
    public Pagina listar(String pessoa,String id,String cursor,int tamanho) {
        if(tamanho<1||tamanho>100)throw new IllegalArgumentException();
        var acesso=acesso(pessoa);var canal=canal(pessoa,acesso,id);Mensagem antes=null;
        if(cursor!=null) {
            antes=chat.buscar(cursor).filter(m->Objects.equals(m.equipeId(),equipe(canal))).orElseThrow(()->ProblemaDominio.naoEncontrado("Página de mensagens não encontrada."));
        }
        var mensagens=chat.listar(equipe(canal),antes,tamanho+1);boolean mais=mensagens.size()>tamanho;
        var pagina=new ArrayList<>(mensagens.subList(0,Math.min(tamanho,mensagens.size())));
        String proxima=mais?pagina.get(pagina.size()-1).id():null;
        Collections.reverse(pagina);
        return new Pagina(pagina.stream().map(m->resposta(pessoa,acesso,canal,m)).toList(),proxima,enviar(acesso,canal),canal);
    }
    private String texto(String valor) {
        if(valor==null||valor.isBlank()||valor.length()>2000)throw new IllegalArgumentException();return valor.trim();
    }
    @Transactional
    public Resposta enviar(String pessoa,String id,String texto) {
        bloqueio.adquirir();var acesso=acesso(pessoa);var canal=canal(pessoa,acesso,id);
        if(!enviar(acesso,canal))throw new AccessDeniedException("Acesso negado.");
        String mensagem=UUID.randomUUID().toString();chat.inserir(mensagem,equipe(canal),pessoa,texto(texto));
        // A mensagem já é o registro da conversa; auditoria registra somente alterações e moderação.
        return resposta(pessoa,acesso,canal,chat.buscar(mensagem).orElseThrow());
    }
    private Mensagem buscar(String id,long versao) {
        var mensagem=chat.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Mensagem não encontrada."));
        if(versao<0)throw new IllegalArgumentException();return mensagem;
    }
    private void versao(Mensagem mensagem,long versao) {
        if(mensagem.removida()||mensagem.versao()!=versao)throw ProblemaDominio.conflito("Esta mensagem foi alterada. Atualize a conversa e tente novamente.");
    }
    @Transactional
    public Resposta editar(String pessoa,String id,String texto,long versao) {
        bloqueio.adquirir();var acesso=acesso(pessoa);var mensagem=buscar(id,versao);var canal=canal(pessoa,acesso,canal(mensagem));
        if(!enviar(acesso,canal)||!mensagem.autora().id().equals(pessoa))throw new AccessDeniedException("Acesso negado.");
        versao(mensagem,versao);if(chat.editar(id,versao,texto(texto))!=1)throw ProblemaDominio.conflito("Esta mensagem foi alterada. Atualize a conversa e tente novamente.");
        auditoria.registrar(pessoa,"chat.message_updated","ChatMessage",id);return resposta(pessoa,acesso,canal,chat.buscar(id).orElseThrow());
    }
    @Transactional
    public Resposta remover(String pessoa,String id,long versao) {
        bloqueio.adquirir();var acesso=acesso(pessoa);var mensagem=buscar(id,versao);var canal=canal(pessoa,acesso,canal(mensagem));
        if(canal.arquivado()||!(enviar(acesso,canal)&&mensagem.autora().id().equals(pessoa))&&!acesso.permissoes().contains("chat.moderate"))throw new AccessDeniedException("Acesso negado.");
        versao(mensagem,versao);if(chat.remover(id,versao,pessoa)!=1)throw ProblemaDominio.conflito("Esta mensagem foi alterada. Atualize a conversa e tente novamente.");
        auditoria.registrar(pessoa,"chat.message_removed","ChatMessage",id);return resposta(pessoa,acesso,canal,chat.buscar(id).orElseThrow());
    }
}
