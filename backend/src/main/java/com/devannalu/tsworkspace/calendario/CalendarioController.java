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
    public CalendarioController(CalendarioService calendario) { this.calendario = calendario; }
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
