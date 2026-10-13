package com.devannalu.tsworkspace.chat;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chat;
    public ChatController(ChatService chat) {this.chat=chat;}
    public record Dados(@NotBlank @Size(max=2000) String texto,@Min(0) Long versao) { }
    public record Versao(@NotNull @Min(0) Long versao) { }
    @GetMapping("/channels")
    public List<ChatRepository.Canal> canais(@AuthenticationPrincipal AppUserPrincipal pessoa) {return chat.canais(pessoa.id());}
    @GetMapping("/channels/{canal}/messages")
    public ChatService.Pagina listar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable String canal,@RequestParam(required=false) UUID before,@RequestParam(defaultValue="50") int size) {
        return chat.listar(pessoa.id(),canal,before==null?null:before.toString(),size);
    }
    @PostMapping("/channels/{canal}/messages") @ResponseStatus(HttpStatus.CREATED)
    public ChatService.Resposta enviar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable String canal,@Valid @RequestBody Dados dados) {return chat.enviar(pessoa.id(),canal,dados.texto());}
    @PatchMapping("/messages/{id}")
    public ChatService.Resposta editar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Dados dados) {
        if(dados.versao()==null)throw new IllegalArgumentException();return chat.editar(pessoa.id(),id.toString(),dados.texto(),dados.versao());
    }
    @PostMapping("/messages/{id}/remove")
    public ChatService.Resposta remover(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Versao dados) {return chat.remover(pessoa.id(),id.toString(),dados.versao());}
}
