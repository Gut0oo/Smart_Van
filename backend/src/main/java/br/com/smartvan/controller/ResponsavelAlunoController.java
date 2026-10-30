package br.com.smartvan.controller;

import br.com.smartvan.dto.AlunoRequestDTO;
import br.com.smartvan.dto.AlunoResponseDTO;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.security.Papel;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.AlunoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/responsavel/alunos")
@Papel(TipoUsuario.RESPONSAVEL)
public class ResponsavelAlunoController {

    private final AlunoService service;

    public ResponsavelAlunoController(AlunoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlunoResponseDTO cadastrar(@Valid @RequestBody AlunoRequestDTO req) {
        return service.cadastrar(UsuarioLogado.atual().id(), req);
    }

    @GetMapping
    public List<AlunoResponseDTO> listar() {
        return service.listar(UsuarioLogado.atual().id());
    }
}
