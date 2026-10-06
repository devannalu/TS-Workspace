package com.devannalu.tsworkspace.anexos;

import com.devannalu.tsworkspace.anexos.Anexo.*;
import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import com.devannalu.tsworkspace.compartilhado.*;
import com.devannalu.tsworkspace.projetos.ProjetoService;
import com.devannalu.tsworkspace.tarefas.*;
import com.devannalu.tsworkspace.rbac.PermissaoService;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AnexoService {
    private final AnexoRepository anexos;
    private final ArmazenamentoArquivo armazenamento;
    private final TarefaService tarefas;
    private final ProjetoService projetos;
    private final TarefaRepository equipes;
    private final PermissaoService permissoes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    private final TransactionTemplate transacao;
    public AnexoService(AnexoRepository anexos,ArmazenamentoArquivo armazenamento,TarefaService tarefas,
        ProjetoService projetos,TarefaRepository equipes,PermissaoService permissoes,BloqueioOrganizacao bloqueio,
        AuditoriaRepository auditoria,PlatformTransactionManager gerente) {
        this.anexos=anexos;this.armazenamento=armazenamento;this.tarefas=tarefas;this.projetos=projetos;
        this.equipes=equipes;this.permissoes=permissoes;this.bloqueio=bloqueio;this.auditoria=auditoria;
        transacao=new TransactionTemplate(gerente);
    }
    public record Pessoa(String id,String nome) { }
    public record AnexoResponse(String id,String nomeOriginal,String tipoMime,long tamanhoBytes,Pessoa enviadaPor,
        Instant criadaEm,boolean podeRemover) { }
    public record PaginaAnexos(List<AnexoResponse> items,long total,int page,int size,boolean podeAnexar) { }
    public record UploadResponse(String attachmentId,String uploadUrl,Map<String,String> headers,Instant expiresAt) { }
    public record DownloadResponse(String downloadUrl,Instant expiresAt) { }
    private record Acesso(boolean anexar,boolean moderar) { }
    private Acesso acesso(String usuario,Recurso recurso,String id) {
        boolean arquivado;String equipe;
        if(recurso==Recurso.TAREFA) {var t=tarefas.buscarTarefa(usuario,id);arquivado=t.arquivada();equipe=t.equipe().id();}
        else {var p=projetos.buscarProjeto(usuario,id);arquivado=p.arquivado();equipe=p.equipe().id();}
        boolean somenteLeitura=arquivado||equipes.equipeArquivada(equipe).orElseThrow();
        var p=permissoes.buscarPermissoesUsuario(usuario);String perfil=p.role().key();var chaves=p.chavesEfetivas();
        boolean suporteProjeto=recurso==Recurso.PROJETO&&perfil.equals("SUPPORT");
        return new Acesso(!somenteLeitura&&!suporteProjeto&&chaves.contains(recurso.rota+".attach"),
            !suporteProjeto&&chaves.contains("attachments.remove")&&(!somenteLeitura||perfil.equals("ADMIN")||perfil.equals("SUPER_ADMIN")));
    }
    private Anexo buscar(String id) {return anexos.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Anexo não encontrado."));}
    private AnexoResponse resposta(String usuario,Acesso acesso,Anexo a) {
        return new AnexoResponse(a.id(),a.nomeOriginal(),a.tipoMime(),a.tamanhoBytes(),new Pessoa(a.enviadaPorId(),a.enviadaPorNome()),a.criadaEm(),
            acesso.moderar()||(acesso.anexar()&&usuario.equals(a.enviadaPorId())));
    }
    public PaginaAnexos listar(String usuario,Recurso recurso,String id,int pagina,int tamanho) {
        if(pagina<0||pagina>100000||tamanho<1||tamanho>100)throw new IllegalArgumentException();
        var acesso=acesso(usuario,recurso,id);
        return new PaginaAnexos(anexos.listar(recurso,id,pagina,tamanho).stream().map(a->resposta(usuario,acesso,a)).toList(),
            anexos.contar(recurso,id),pagina,tamanho,acesso.anexar());
    }
    private static final Map<String,String> TIPOS=Map.of("pdf","application/pdf","png","image/png","jpg","image/jpeg",
        "jpeg","image/jpeg","webp","image/webp","txt","text/plain","csv","text/csv");
    private void validar(String nome,String mime,long tamanho) {
        if(nome==null||nome.isBlank()||nome.length()>255||nome.contains("/")||nome.contains("\\")||nome.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException();
        int ponto=nome.lastIndexOf('.');String extensao=ponto<0?"":nome.substring(ponto+1).toLowerCase(Locale.ROOT);
        if(!Objects.equals(TIPOS.get(extensao),mime)||mime==null)throw ProblemaDominio.conflito("Este tipo de arquivo não é permitido.");
        if(tamanho<1||tamanho>ArmazenamentoArquivo.LIMITE)throw ProblemaDominio.conflito("O arquivo ultrapassa o limite de 10 MB.");
    }
    public UploadResponse criarUpload(String usuario,Recurso recurso,String recursoId,String nome,String mime,long tamanho) {
        return criarUpload(usuario,recurso,recursoId,nome,mime,tamanho,null);
    }
    public UploadResponse criarUpload(String usuario,Recurso recurso,String recursoId,String nome,String mime,long tamanho,UUID solicitacaoId) {
        return transacao.execute(status->{
            bloqueio.adquirir();if(!acesso(usuario,recurso,recursoId).anexar())throw new AccessDeniedException("Acesso negado.");
            validar(nome,mime,tamanho);
            // Identificador de repetição não escolhe a chave de storage nem permite cruzar usuárias.
            String id=solicitacaoId==null?UUID.randomUUID().toString():UUID.nameUUIDFromBytes(
                (usuario+":"+recurso.rota+":"+recursoId+":"+solicitacaoId).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
            var anterior=anexos.buscar(id);
            if(anterior.isPresent()) {
                var a=anterior.get();
                if(a.estado()!=Estado.PENDENTE||!Instant.now().isBefore(a.uploadExpiraEm()))
                    throw ProblemaDominio.conflito("A autorização de upload expirou ou já foi concluída.");
                if(!a.nomeOriginal().equals(nome)||!a.tipoMime().equals(mime)||a.tamanhoBytes()!=tamanho)
                    throw ProblemaDominio.conflito("A solicitação de upload não corresponde ao arquivo original.");
                var upload=armazenamento.criarUpload(a);
                return new UploadResponse(id,upload.url(),upload.headers(),a.uploadExpiraEm());
            }
            Instant expira=Instant.now().plus(armazenamento.validade());
            anexos.inserir(id,recurso,recursoId,usuario,nome,recurso.rota+"/"+recursoId+"/"+UUID.randomUUID(),mime,tamanho,expira);
            var upload=armazenamento.criarUpload(buscar(id));return new UploadResponse(id,upload.url(),upload.headers(),expira);
        });
    }
    public AnexoResponse confirmar(String usuario,Recurso recurso,String recursoId,String id) {
        return transacao.execute(status->{
            bloqueio.adquirir();var acesso=acesso(usuario,recurso,recursoId);var a=buscar(id);
            if(a.recurso()!=recurso||!a.recursoId().equals(recursoId))throw ProblemaDominio.naoEncontrado("Anexo não encontrado.");
            if(!a.enviadaPorId().equals(usuario)||!acesso.anexar())throw new AccessDeniedException("Acesso negado.");
            if(a.estado()==Estado.REMOVIDO)throw ProblemaDominio.naoEncontrado("Anexo não encontrado.");
            if(a.estado()==Estado.DISPONIVEL)return resposta(usuario,acesso,a);
            if(!Instant.now().isBefore(a.uploadExpiraEm()))throw ProblemaDominio.conflito("A autorização de upload expirou.");
            armazenamento.confirmarUpload(a);anexos.disponibilizar(id);auditoria.registrar(usuario,"attachment.created","Attachment",id);
            return resposta(usuario,acesso,buscar(id));
        });
    }
    public DownloadResponse download(String usuario,String id) {
        var a=buscar(id);acesso(usuario,a.recurso(),a.recursoId());
        if(a.estado()!=Estado.DISPONIVEL)throw ProblemaDominio.naoEncontrado("Anexo não encontrado.");
        return new DownloadResponse(armazenamento.gerarDownload(a),Instant.now().plus(armazenamento.validade()));
    }
    public void remover(String usuario,String id) {
        var a=transacao.execute(status->{
            bloqueio.adquirir();var atual=buscar(id);var acesso=acesso(usuario,atual.recurso(),atual.recursoId());
            if(!acesso.moderar()&&!(acesso.anexar()&&atual.enviadaPorId().equals(usuario)))throw new AccessDeniedException("Acesso negado.");
            if(anexos.remover(id)==1)auditoria.registrar(usuario,"attachment.removed","Attachment",id);
            return atual;
        });
        // Remoção lógica permanece mesmo se o storage falhar; repetir conclui a limpeza.
        armazenamento.removerObjeto(a);
    }
}
