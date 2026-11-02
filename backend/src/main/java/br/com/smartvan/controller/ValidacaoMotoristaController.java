package br.com.smartvan.controller;

import br.com.smartvan.dto.UsuarioDetalheResponseDTO;
import br.com.smartvan.dto.ValidacaoRequestDTO;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.security.Papel;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.ValidacaoMotoristaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/motoristas")
@Papel(TipoUsuario.ADMINISTRADOR)
public class ValidacaoMotoristaController {

    private final ValidacaoMotoristaService service;

    public ValidacaoMotoristaController(ValidacaoMotoristaService service) {
        this.service = service;
    }

    @GetMapping("/pendentes")
    public List<UsuarioDetalheResponseDTO> pendentes() {
        return service.listarPendentes();
    }

    @PostMapping("/{id}/validacao")
    public UsuarioDetalheResponseDTO decidir(@PathVariable Long id, @Valid @RequestBody ValidacaoRequestDTO req) {
        return service.decidir(UsuarioLogado.atual().id(), id, req);
    }
}
