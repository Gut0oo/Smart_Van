package br.com.smartvan.service;

import br.com.smartvan.dto.UsuarioRequestDTO;
import br.com.smartvan.dto.UsuarioResponseDTO;
import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponseDTO salvarUsuario(@RequestBody UsuarioRequestDTO dto){
        Usuario usuario = new Usuario();

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setCpf(dto.cpf());
        usuario.setTelefone(dto.telefone());
        usuario.setTipo(dto.tipo());

        usuario.setStatus(StatusConta.PENDENTE);//status inicial

        String senhaHash = passwordEncoder.encode(dto.senha());
        usuario.setSenha(senhaHash);

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        return new UsuarioResponseDTO(usuarioSalvo.getUsuario_id(), usuarioSalvo.getNome(), usuarioSalvo.getEmail(), usuarioSalvo.getTipo(), usuarioSalvo.getStatus(), usuarioSalvo.getDataCriacao(), usuarioSalvo.getUltimoAcesso());
    }
}
