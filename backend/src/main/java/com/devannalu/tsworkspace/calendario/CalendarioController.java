package com.devannalu.tsworkspace.calendario;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarioController {
    private final CalendarioService calendario;
    private final ExportacaoCalendario exportacao;
    public CalendarioController(CalendarioService calendario,ExportacaoCalendario exportacao) { this.calendario = calendario;this.exportacao=exportacao; }
    @GetMapping(value="/export",produces="text/calendar;charset=UTF-8")
    public org.springframework.http.ResponseEntity<String> exportar(@AuthenticationPrincipal AppUserPrincipal usuaria,
        @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required=false) UUID teamId,@RequestParam(required=false) Set<CalendarioService.Tipo> types,@RequestParam(required=false) UUID responsibleId) {
        var arquivo=exportacao.exportar(usuaria.id(),from,to,teamId==null?null:teamId.toString(),types,responsibleId==null?null:responsibleId.toString());
        return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"ts-workspace.ics\"").body(arquivo);
    }
    @GetMapping
    public List<CalendarioService.Item> listar(@AuthenticationPrincipal AppUserPrincipal usuaria,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) UUID teamId,
        @RequestParam(required = false) Set<CalendarioService.Tipo> types,
        @RequestParam(required = false) UUID responsibleId) {
        return calendario.listar(usuaria.id(), from, to, teamId == null ? null : teamId.toString(),
            types, responsibleId == null ? null : responsibleId.toString());
    }
}
