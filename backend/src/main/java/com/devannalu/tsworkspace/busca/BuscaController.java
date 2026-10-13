package com.devannalu.tsworkspace.busca;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
public class BuscaController {
    private final BuscaService busca;
    public BuscaController(BuscaService busca) {this.busca=busca;}
    @GetMapping
    public BuscaService.Resposta buscar(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam String q,
        @RequestParam(required=false) BuscaRepository.Tipo type,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return busca.buscar(pessoa.id(),q,type,page,size);
    }
}
