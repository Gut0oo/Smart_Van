package br.com.smartvan.controller;

import br.com.smartvan.dto.UsuarioRequestDTO;
import br.com.smartvan.dto.UsuarioResponseDTO;
import br.com.smartvan.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO cadastrar(@RequestBody UsuarioRequestDTO dto) {

        return usuarioService.salvarUsuario(dto);
    }

}
