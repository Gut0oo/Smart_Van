package br.com.smartvan.controller;

import br.com.smartvan.dto.EscolaRequestDTO;
import br.com.smartvan.dto.EscolaResponseDTO;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.security.Papel;
import br.com.smartvan.service.EscolaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//seção 1 do contrato; a listagem é pública (ver InterceptorUsuarioLogado) e o cadastro é do administrador
@RestController
@RequestMapping("/escolas")
public class EscolaController {

    private final EscolaService service;

    public EscolaController(EscolaService service) {
        this.service = service;
    }

    @PostMapping
    @Papel(TipoUsuario.ADMINISTRADOR)
    @ResponseStatus(HttpStatus.CREATED)
    public EscolaResponseDTO cadastrar(@Valid @RequestBody EscolaRequestDTO req) {
        return service.cadastrar(req);
    }

    @GetMapping
    public List<EscolaResponseDTO> listar(@RequestParam(required = false) String cidade,
                                          @RequestParam(required = false) String nome) {
        return service.listar(cidade, nome);
    }
}
