package br.com.smartvan.controller;

import br.com.smartvan.dto.GerarRotaRequestDTO;
import br.com.smartvan.dto.RotaResponseDTO;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.security.Papel;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.RotaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/motorista/rotas")
@Papel(TipoUsuario.MOTORISTA)
public class RotaController {

    private final RotaService service;

    public RotaController(RotaService service) {
        this.service = service;
    }

    @PostMapping("/gerar")
    public ResponseEntity<RotaResponseDTO> gerar(@Valid @RequestBody GerarRotaRequestDTO req) {
        RotaService.Resultado resultado = service.gerar(UsuarioLogado.atual().id(), req);
        return ResponseEntity.status(resultado.nova() ? HttpStatus.CREATED : HttpStatus.OK).body(resultado.rota());
    }

    @GetMapping
    public List<RotaResponseDTO> listar(@RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.listar(UsuarioLogado.atual().id(), data);
    }

    @GetMapping("/{id}")
    public RotaResponseDTO buscar(@PathVariable Long id) {
        return service.buscar(UsuarioLogado.atual().id(), id);
    }
}
