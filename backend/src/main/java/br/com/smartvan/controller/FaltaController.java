package br.com.smartvan.controller;

import br.com.smartvan.dto.FaltaRequestDTO;
import br.com.smartvan.dto.FaltaResponseDTO;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.security.Papel;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.FaltaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/alunos/{alunoId}/faltas")
@Papel(TipoUsuario.RESPONSAVEL)
public class FaltaController {

    private final FaltaService service;

    public FaltaController(FaltaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FaltaResponseDTO registrar(@PathVariable Long alunoId, @Valid @RequestBody FaltaRequestDTO req) {
        return service.registrar(UsuarioLogado.atual().id(), alunoId, req);
    }

    @GetMapping
    public List<FaltaResponseDTO> listar(@PathVariable Long alunoId, @RequestParam(required = false) YearMonth mes) {
        return service.listar(UsuarioLogado.atual().id(), alunoId, mes);
    }

    @DeleteMapping("/{data}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long alunoId,
                        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        service.remover(UsuarioLogado.atual().id(), alunoId, data);
    }
}
