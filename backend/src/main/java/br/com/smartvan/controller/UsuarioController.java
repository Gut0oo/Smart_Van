package br.com.smartvan.controller;

import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public Usuario salvarUsuario(@RequestBody Usuario usuario){
        System.out.println("Usuario recebido: " + usuario);

        return usuarioRepository.save(usuario);
    }
}
