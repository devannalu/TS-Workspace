package com.devannalu.tsworkspace.calendario;

import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportacaoCalendario {
    public static final int LIMITE_ITENS=5000;
    private static final DateTimeFormatter INSTANTE=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private final CalendarioService calendario;
    private final String origem;
    public ExportacaoCalendario(CalendarioService calendario,@Value("${app.frontend-origin}") String origem) {
        this.calendario=calendario;this.origem=origem.replaceAll("/$","");
    }
    @Transactional(readOnly=true)
    public String exportar(String pessoa,LocalDate de,LocalDate ate,String equipe,Set<CalendarioService.Tipo> tipos,String responsavel) {
        var itens=calendario.listar(pessoa,de,ate,equipe,tipos,responsavel);
        if(itens.size()>LIMITE_ITENS)throw ProblemaDominio.requisicaoInvalida("Há muitos itens para exportar. Reduza o período ou aplique filtros.");
        return montar(itens,Instant.now());
    }
    String montar(List<CalendarioService.Item> itens,Instant agora) {
        var linhas=new ArrayList<String>();Collections.addAll(linhas,"BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//Tech Sisters//TS Workspace//PT-BR","CALSCALE:GREGORIAN","METHOD:PUBLISH","X-WR-CALNAME:TS Workspace");
        for(var item:itens) {
            linhas.add("BEGIN:VEVENT");linhas.add("UID:"+item.tipo().name().toLowerCase(Locale.ROOT)+"-"+item.recursoId()+"@ts-workspace.local");
            linhas.add("DTSTAMP:"+INSTANTE.format(agora));linhas.add("CLASS:PRIVATE");
            if(item.inicioEm()!=null) {
                linhas.add("DTSTART:"+INSTANTE.format(item.inicioEm()));linhas.add("DTEND:"+INSTANTE.format(item.fimEm()));
                linhas.add("X-TS-TIMEZONE:"+escapar(item.zona()));linhas.add("TRANSP:OPAQUE");
            }else {
                linhas.add("DTSTART;VALUE=DATE:"+DateTimeFormatter.BASIC_ISO_DATE.format(item.dataInicio()));
                linhas.add("DTEND;VALUE=DATE:"+DateTimeFormatter.BASIC_ISO_DATE.format(item.dataFim().plusDays(1)));linhas.add("TRANSP:TRANSPARENT");
            }
            linhas.add("SUMMARY:"+escapar(item.titulo()));
            linhas.add("DESCRIPTION:"+escapar("Equipe: "+item.equipe().nome()));
            String rota=switch(item.tipo()) {case TAREFA->"tarefas";case PROJETO->"projetos";case REUNIAO->"reunioes";case EVENTO->"eventos";case CONTEUDO->"conteudos";};
            linhas.add("URL:"+origem+"/"+rota+"?abrir="+item.recursoId());linhas.add("END:VEVENT");
        }
        linhas.add("END:VCALENDAR");var resultado=new StringBuilder();
        for(var linha:linhas)resultado.append(dobrar(linha)).append("\r\n");
        return resultado.toString();
    }
    private String escapar(String texto) {
        return texto.replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]","").replace("\\","\\\\").replace("\r\n","\n").replace("\r","\n").replace("\n","\\n").replace(",","\\,").replace(";","\\;");
    }
    private String dobrar(String linha) {
        var resultado=new StringBuilder();int octetos=0;
        // A dobra considera bytes UTF-8 e preserva cada caractere, inclusive emoji.
        for(int indice=0;indice<linha.length();) {
            int codigo=linha.codePointAt(indice);String caractere=new String(Character.toChars(codigo));int tamanho=caractere.getBytes(StandardCharsets.UTF_8).length;
            if(octetos+tamanho>75) {resultado.append("\r\n ");octetos=1;}
            resultado.append(caractere);octetos+=tamanho;indice+=Character.charCount(codigo);
        }
        return resultado.toString();
    }
}
