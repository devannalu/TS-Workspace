package com.devannalu.tsworkspace.calendario;

import com.devannalu.tsworkspace.calendario.CalendarioService.*;
import com.devannalu.tsworkspace.compartilhado.ProblemaDominio;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExportacaoCalendarioTest {
    final CalendarioService calendario=mock(CalendarioService.class);
    final ExportacaoCalendario exportacao=new ExportacaoCalendario(calendario,"https://workspace.example.test");
    private Item item(String titulo,Tipo tipo,Instant inicio,Instant fim,String zona) {
        return new Item(tipo+":id",tipo,"d3a74bba-d200-4cd0-950c-a2adefdd4a37",titulo,LocalDate.of(2026,10,25),LocalDate.of(2026,10,27),
            new Referencia("equipe","Comunicação"),"PLANEJADO",null,List.of(),false,false,inicio,fim,zona);
    }
    @Test void dataCivilPreservaDiasEFimExclusivo() {
        String arquivo=exportacao.montar(List.of(item("Projeto",Tipo.PROJETO,null,null,null)),Instant.EPOCH);
        assertThat(arquivo).contains("DTSTART;VALUE=DATE:20261025\r\n","DTEND;VALUE=DATE:20261028\r\n","CLASS:PRIVATE","TRANSP:TRANSPARENT");
        assertThat(arquivo).endsWith("END:VCALENDAR\r\n");assertThat(arquivo.replace("\r\n","")).doesNotContain("\r","\n");
    }
    @Test void horariosSaoUtcIndependentesDaJvmEPreservamZona() {
        var anterior=TimeZone.getDefault();
        try {
            for(String zona:List.of("America/Bahia","Europe/Lisbon","Pacific/Kiritimati")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zona));
                var encontro=item("Encontro",Tipo.REUNIAO,Instant.parse("2026-10-25T12:00:00Z"),Instant.parse("2026-10-25T13:00:00Z"),"America/Bahia");
                assertThat(exportacao.montar(List.of(encontro),Instant.EPOCH)).contains("DTSTART:20261025T120000Z\r\n","DTEND:20261025T130000Z\r\n","X-TS-TIMEZONE:America/Bahia");
            }
        }finally {TimeZone.setDefault(anterior);}
    }
    @Test void textoNaoInjetaPropriedadesEUnicodeNaoQuebraDobraUtf8() {
        String titulo="Planejar\\texto,\r\nBEGIN:VEVENT;"+(char)0+"😀á".repeat(80);
        String arquivo=exportacao.montar(List.of(item(titulo,Tipo.TAREFA,null,null,null)),Instant.EPOCH);
        assertThat(arquivo.lines().filter(l->l.equals("BEGIN:VEVENT")).count()).isEqualTo(1);
        for(String linha:arquivo.split("\r\n"))assertThat(linha.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        String completo=arquivo.replace("\r\n ","");
        assertThat(completo).contains("SUMMARY:Planejar\\\\texto\\,\\nBEGIN:VEVENT\\;"+"😀á".repeat(80)).doesNotContain(Character.toString(0),"�");
    }
    @Test void uidEstavelELinkSemToken() {
        var item=item("Projeto",Tipo.PROJETO,null,null,null);
        var primeiro=exportacao.montar(List.of(item),Instant.EPOCH);var segundo=exportacao.montar(List.of(item),Instant.now());
        assertThat(primeiro.lines().filter(l->l.startsWith("UID:")).toList()).isEqualTo(segundo.lines().filter(l->l.startsWith("UID:")).toList());
        assertThat(primeiro.replace("\r\n ","")).contains("URL:https://workspace.example.test/projetos?abrir="+item.recursoId()).doesNotContain("token","ATTENDEE","ORGANIZER");
    }
    @Test void exportacaoDelegaAutorizacaoEFiltrosENaoTruncaExcesso() {
        LocalDate de=LocalDate.of(2026,10,1),ate=LocalDate.of(2026,10,31);var tipos=Set.of(Tipo.TAREFA);
        when(calendario.listar("pessoa",de,ate,"equipe",tipos,"responsavel")).thenReturn(Collections.nCopies(5001,item("Tarefa",Tipo.TAREFA,null,null,null)));
        assertThatThrownBy(()->exportacao.exportar("pessoa",de,ate,"equipe",tipos,"responsavel")).isInstanceOf(ProblemaDominio.class).hasMessageContaining("Reduza o período");
        verify(calendario).listar("pessoa",de,ate,"equipe",tipos,"responsavel");
    }
}
