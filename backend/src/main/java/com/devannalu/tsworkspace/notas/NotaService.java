package com.devannalu.tsworkspace.notas;

import com.devannalu.tsworkspace.notas.NotaRepository.*;
import com.devannalu.tsworkspace.compartilhado.*;
import com.devannalu.tsworkspace.auditoria.AuditoriaRepository;
import com.devannalu.tsworkspace.projetos.ProjetoService;
import com.devannalu.tsworkspace.eventos.EventoService;
import com.devannalu.tsworkspace.parcerias.ParceriaService;
import com.devannalu.tsworkspace.reunioes.ReuniaoService;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotaService {
    private final NotaRepository notas;
    private final ProjetoService projetos;
    private final EventoService eventos;
    private final ParceriaService parcerias;
    private final ReuniaoService reunioes;
    private final BloqueioOrganizacao bloqueio;
    private final AuditoriaRepository auditoria;
    public NotaService(NotaRepository notas, ProjetoService projetos, EventoService eventos, ParceriaService parcerias,
        ReuniaoService reunioes, BloqueioOrganizacao bloqueio, AuditoriaRepository auditoria) {
        this.notas=notas; this.projetos=projetos; this.eventos=eventos; this.parcerias=parcerias;
        this.reunioes=reunioes; this.bloqueio=bloqueio; this.auditoria=auditoria;
    }
    public record Resposta(String id, Pessoa autora, String titulo, String texto, long versao,
        Instant criadaEm, Instant atualizadaEm, boolean editar, boolean remover) { }
    public record Pagina(List<Resposta> items, long total, int page, int size, boolean podeCriar) { }
    private boolean acesso(String pessoa, Recurso recurso, String id) {
        // A fonte revalida leitura, escopo, arquivo e capacidades na sessão atual.
        return switch(recurso) {
            case PROJETO -> projetos.buscarProjeto(pessoa,id).capacidades().editar();
            case EVENTO -> eventos.detalhe(pessoa,id).capacidades().editar();
            case PARCERIA -> parcerias.detalhe(pessoa,id).capacidades().editar();
            case REUNIAO -> reunioes.detalhe(pessoa,id).capacidades().editar();
        };
    }
    private Resposta resposta(String pessoa, boolean podeEscrever, Registro nota) {
        boolean propria=podeEscrever && nota.autora().id().equals(pessoa) && nota.removidaEm()==null;
        return new Resposta(nota.id(),nota.autora(),nota.titulo(),nota.texto(),nota.versao(),nota.criadaEm(),nota.atualizadaEm(),propria,propria);
    }
    private void validar(String titulo, String texto) {
        if(titulo==null||titulo.isBlank()||titulo.length()>200||texto==null||texto.isBlank()||texto.length()>5000) throw new IllegalArgumentException();
    }
    @Transactional(readOnly=true)
    public Resposta detalhe(String pessoa, Recurso recurso, String recursoId, String id) {
        boolean escrever=acesso(pessoa,recurso,recursoId);
        var nota=notas.buscar(id).filter(n->n.recurso()==recurso&&n.recursoId().equals(recursoId)&&n.removidaEm()==null)
            .orElseThrow(()->ProblemaDominio.naoEncontrado("Nota não encontrada."));
        return resposta(pessoa,escrever,nota);
    }
    @Transactional(readOnly=true)
    public Pagina listar(String pessoa, Recurso recurso, String id, int pagina, int tamanho) {
        if(pagina<0||pagina>100000||tamanho<1||tamanho>100) throw new IllegalArgumentException();
        boolean escrever=acesso(pessoa,recurso,id);
        return new Pagina(notas.listar(recurso,id,pagina,tamanho).stream().map(n->resposta(pessoa,escrever,n)).toList(),notas.contar(recurso,id),pagina,tamanho,escrever);
    }
    @Transactional
    public Resposta criar(String pessoa, Recurso recurso, String recursoId, String titulo, String texto) {
        bloqueio.adquirir();
        if(!acesso(pessoa,recurso,recursoId)) throw new AccessDeniedException("Acesso negado.");
        validar(titulo,texto); String id=UUID.randomUUID().toString();
        notas.inserir(id,recurso,recursoId,pessoa,titulo.trim(),texto.trim());
        auditoria.registrar(pessoa,"note.created","Note",id);
        return resposta(pessoa,true,notas.buscar(id).orElseThrow());
    }
    private Registro propria(String pessoa, String id, long versao) {
        var nota=notas.buscar(id).orElseThrow(()->ProblemaDominio.naoEncontrado("Nota não encontrada."));
        if(!acesso(pessoa,nota.recurso(),nota.recursoId())||!nota.autora().id().equals(pessoa)) throw new AccessDeniedException("Acesso negado.");
        if(versao<0) throw new IllegalArgumentException();
        if(nota.removidaEm()!=null||nota.versao()!=versao) throw ProblemaDominio.conflito("Esta nota foi alterada. Atualize os dados e tente novamente.");
        return nota;
    }
    @Transactional
    public Resposta editar(String pessoa, String id, String titulo, String texto, long versao) {
        bloqueio.adquirir(); propria(pessoa,id,versao); validar(titulo,texto);
        if(notas.editar(id,versao,titulo.trim(),texto.trim())!=1) throw ProblemaDominio.conflito("Esta nota foi alterada. Atualize os dados e tente novamente.");
        auditoria.registrar(pessoa,"note.updated","Note",id);
        return resposta(pessoa,true,notas.buscar(id).orElseThrow());
    }
    @Transactional
    public void remover(String pessoa, String id, long versao) {
        bloqueio.adquirir(); propria(pessoa,id,versao);
        if(notas.remover(id,versao)!=1) throw ProblemaDominio.conflito("Esta nota foi alterada. Atualize os dados e tente novamente.");
        auditoria.registrar(pessoa,"note.removed","Note",id);
    }
}
