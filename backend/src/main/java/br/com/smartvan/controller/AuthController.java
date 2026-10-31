package br.com.smartvan.controller;

import br.com.smartvan.dto.LoginRequestDTO;
import br.com.smartvan.dto.LoginResponseDTO;
import br.com.smartvan.dto.UsuarioDetalheResponseDTO;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

//seção 1 do contrato
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO req) {
        return service.login(req);
    }

    @GetMapping("/me")
    public UsuarioDetalheResponseDTO me() {
        return service.me(UsuarioLogado.atual().id());
    }
}
