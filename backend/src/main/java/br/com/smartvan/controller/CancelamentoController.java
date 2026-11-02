package br.com.smartvan.controller;

import br.com.smartvan.dto.CancelamentoRequestDTO;
import br.com.smartvan.dto.ContratoResponseDTO;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.CancelamentoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CancelamentoController {

    private final CancelamentoService service;

    public CancelamentoController(CancelamentoService service) {
        this.service = service;
    }

    //sem @Papel: vale para responsável e motorista, e a conferência de parte fica no service
    @PostMapping("/contratos/{id}/cancelamento")
    public ContratoResponseDTO solicitar(@PathVariable Long id, @Valid @RequestBody CancelamentoRequestDTO req) {
        return service.solicitar(UsuarioLogado.atual(), id, req.motivo());
    }
}
