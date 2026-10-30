package br.com.smartvan.controller;

import br.com.smartvan.dto.ContagemResponseDTO;
import br.com.smartvan.dto.NotificacaoResponseDTO;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//seção 6 do contrato; a proteção "logado"/"dono" entra na etapa 2 (login)
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificacaoResponseDTO> listar(@RequestParam(required = false) Boolean lidas) {
        return service.listar(UsuarioLogado.atual().id(), lidas);
    }

    @GetMapping("/nao-lidas/contagem")
    public ContagemResponseDTO contagem() {
        return new ContagemResponseDTO(service.contarNaoLidas(UsuarioLogado.atual().id()));
    }

    @PostMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable Long id) {
        service.marcarComoLida(UsuarioLogado.atual().id(), id);
        return ResponseEntity.noContent().build();
    }
}
