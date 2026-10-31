package br.com.smartvan.controller;

import br.com.smartvan.dto.AtendimentoResponseDTO;
import br.com.smartvan.dto.AtendimentosRequestDTO;
import br.com.smartvan.dto.EnderecoRequestDTO;
import br.com.smartvan.dto.EnderecoResponseDTO;
import br.com.smartvan.dto.PerfilMotoristaResponseDTO;
import br.com.smartvan.dto.VeiculoRequestDTO;
import br.com.smartvan.dto.VeiculoResponseDTO;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.security.Papel;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.MotoristaCadastroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/motorista")
@Papel(TipoUsuario.MOTORISTA)
public class MotoristaCadastroController {

    private final MotoristaCadastroService service;

    public MotoristaCadastroController(MotoristaCadastroService service) {
        this.service = service;
    }

    @PostMapping("/veiculos")
    @ResponseStatus(HttpStatus.CREATED)
    public VeiculoResponseDTO cadastrarVeiculo(@Valid @RequestBody VeiculoRequestDTO req) {
        return service.cadastrarVeiculo(UsuarioLogado.atual().id(), req);
    }

    @GetMapping("/veiculos")
    public List<VeiculoResponseDTO> listarVeiculos() {
        return service.listarVeiculos(UsuarioLogado.atual().id());
    }

    @PutMapping("/escolas")
    public List<AtendimentoResponseDTO> definirAtendimentos(@Valid @RequestBody AtendimentosRequestDTO req) {
        return service.definirAtendimentos(UsuarioLogado.atual().id(), req.atendimentos());
    }

    @GetMapping("/escolas")
    public List<AtendimentoResponseDTO> listarAtendimentos() {
        return service.listarAtendimentos(UsuarioLogado.atual().id());
    }

    @PutMapping("/endereco-partida")
    public EnderecoResponseDTO definirEnderecoPartida(@Valid @RequestBody EnderecoRequestDTO req) {
        return service.definirEnderecoPartida(UsuarioLogado.atual().id(), req);
    }

    @GetMapping("/perfil")
    public PerfilMotoristaResponseDTO perfil() {
        return service.perfil(UsuarioLogado.atual().id());
    }
}
