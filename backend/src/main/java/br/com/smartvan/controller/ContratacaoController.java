package br.com.smartvan.controller;

import br.com.smartvan.dto.ContratoResponseDTO;
import br.com.smartvan.dto.MotoristaBuscaResponseDTO;
import br.com.smartvan.dto.MotoristaPerfilPublicoResponseDTO;
import br.com.smartvan.dto.PropostaRequestDTO;
import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.security.Papel;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.ContratacaoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
public class ContratacaoController {

    private final ContratacaoService service;

    public ContratacaoController(ContratacaoService service) {
        this.service = service;
    }

    @GetMapping("/motoristas")
    @Papel(TipoUsuario.RESPONSAVEL)
    public List<MotoristaBuscaResponseDTO> buscar(
            @RequestParam(required = false) Long escolaId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horarioAula,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String nome) {
        return service.buscar(UsuarioLogado.atual().id(), escolaId, horarioAula, cidade, nome);
    }

    @GetMapping("/motoristas/{id}")
    @Papel(TipoUsuario.RESPONSAVEL)
    public MotoristaPerfilPublicoResponseDTO perfil(@PathVariable Long id) {
        return service.perfil(UsuarioLogado.atual().id(), id);
    }

    @PostMapping("/contratos")
    @Papel(TipoUsuario.RESPONSAVEL)
    @ResponseStatus(HttpStatus.CREATED)
    public ContratoResponseDTO propor(@Valid @RequestBody PropostaRequestDTO req) {
        return service.propor(UsuarioLogado.atual().id(), req);
    }

    @GetMapping("/contratos")
    public List<ContratoResponseDTO> listar(@RequestParam(required = false) StatusContrato status) {
        return service.listar(UsuarioLogado.atual().id(), status);
    }

    @GetMapping("/contratos/{id}")
    public ContratoResponseDTO obter(@PathVariable Long id) {
        return service.obter(UsuarioLogado.atual(), id);
    }

    @PostMapping("/contratos/{id}/aceitar")
    @Papel(TipoUsuario.MOTORISTA)
    public ContratoResponseDTO aceitar(@PathVariable Long id) {
        return service.aceitar(UsuarioLogado.atual().id(), id);
    }

    @PostMapping("/contratos/{id}/recusar")
    @Papel(TipoUsuario.MOTORISTA)
    public ContratoResponseDTO recusar(@PathVariable Long id) {
        return service.recusar(UsuarioLogado.atual().id(), id);
    }
}
