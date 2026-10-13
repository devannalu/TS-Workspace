package com.devannalu.tsworkspace.busca;

import com.devannalu.tsworkspace.rbac.PermissaoService;
import com.devannalu.tsworkspace.busca.BuscaRepository.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscaService {
    private final BuscaRepository busca;
    private final PermissaoService permissoes;
    public BuscaService(BuscaRepository busca,PermissaoService permissoes) {this.busca=busca;this.permissoes=permissoes;}
    public record Grupo(Tipo tipo,List<Resultado> items,long total,int page,int size) { }
    public record Resposta(String consulta,List<Tipo> tipos,List<Grupo> grupos) { }
    @Transactional(readOnly=true)
    public Resposta buscar(String pessoa,String consulta,Tipo filtro,int pagina,int tamanho) {
        if(consulta==null||consulta.trim().length()<2||consulta.length()>160||pagina<0||pagina>10000||tamanho<1||tamanho>100)throw new IllegalArgumentException();
        var acesso=permissoes.buscarPermissoesUsuario(pessoa);
        if(acesso.role()==null)throw new AccessDeniedException("Acesso negado.");
        var chaves=Set.copyOf(acesso.chavesEfetivas());
        var tipos=new ArrayList<Tipo>();
        for(var tipo:Tipo.values())if(tipo!=Tipo.NOTA&&chaves.contains(tipo.permissao))tipos.add(tipo);
        if(tipos.stream().anyMatch(t->t.colunaNota!=null))tipos.add(Tipo.NOTA);
        if(filtro!=null&&!tipos.contains(filtro))throw new AccessDeniedException("Acesso negado.");
        boolean global=Set.of("SUPER_ADMIN","ADMIN").contains(acesso.role().key());
        var grupos=new ArrayList<Grupo>();
        for(var tipo:filtro==null?tipos:List.of(filtro)) {
            int page=filtro==null?0:pagina,size=filtro==null?5:tamanho;
            var resultado=busca.buscar(tipo,pessoa,global,consulta.trim(),chaves,page,size);
            grupos.add(new Grupo(tipo,resultado.items(),resultado.total(),page,size));
        }
        return new Resposta(consulta.trim(),List.copyOf(tipos),List.copyOf(grupos));
    }
}
