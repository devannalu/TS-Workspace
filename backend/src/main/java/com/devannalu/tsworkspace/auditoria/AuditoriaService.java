package com.devannalu.tsworkspace.auditoria;

import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import com.devannalu.tsworkspace.auditoria.AuditoriaRepository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository auditoria;
    private final PermissaoService permissoes;
    public AuditoriaService(AuditoriaRepository auditoria,PermissaoService permissoes) {this.auditoria=auditoria;this.permissoes=permissoes;}
    public record Pagina(List<Evento> items,long total,int page,int size,Instant desde,Instant ate) { }
    public record Opcoes(List<String> acoes,List<String> tipos) { }
    @Transactional(readOnly=true)
    public Opcoes opcoes(String pessoa) {
        permissoes.exigirPermissao(pessoa,"audit.view");
        return new Opcoes(AuditoriaRepository.ACOES.stream().sorted().toList(),AuditoriaRepository.TIPOS.stream().sorted().toList());
    }
    @Transactional(readOnly=true)
    public Pagina listar(String pessoa,String autora,String acao,String tipo,Instant desde,Instant ate,int pagina,int tamanho) {
        permissoes.exigirPermissao(pessoa,"audit.view");
        Instant fim=ate==null?Instant.now():ate,inicio=desde==null?fim.minus(Duration.ofDays(30)):desde;
        if(!inicio.isBefore(fim)||Duration.between(inicio,fim).compareTo(Duration.ofDays(366))>0||pagina<0||pagina>10000||tamanho<1||tamanho>100
            ||autora!=null&&autora.length()>200||acao!=null&&!acao.matches("[a-zA-Z][a-zA-Z0-9_.]{0,99}")||tipo!=null&&!tipo.matches("[a-zA-Z][a-zA-Z0-9_.]{0,49}"))throw new IllegalArgumentException();
        var filtro=new Filtros(autora,acao,tipo,inicio,fim);
        return new Pagina(auditoria.listar(filtro,pagina,tamanho),auditoria.contar(filtro),pagina,tamanho,inicio,fim);
    }
    @Transactional(readOnly=true)
    public Evento detalhe(String pessoa,String id) {
        permissoes.exigirPermissao(pessoa,"audit.view");
        return auditoria.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Registro não encontrado."));
    }
}
