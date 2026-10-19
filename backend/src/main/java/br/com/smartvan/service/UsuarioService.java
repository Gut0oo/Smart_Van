package br.com.smartvan.service;

import br.com.smartvan.dto.UsuarioRequestDTO;
import br.com.smartvan.dto.UsuarioResponseDTO;
import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Administrador;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponseDTO salvarUsuario(UsuarioRequestDTO dto){
        Usuario usuario = criarPorTipo(dto.tipo());

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setCpf(dto.cpf());
        usuario.setTelefone(dto.telefone());
        usuario.setTipo(dto.tipo());

        usuario.setStatus(StatusConta.PENDENTE);//status inicial

        String senhaHash = passwordEncoder.encode(dto.senha());
        usuario.setSenha(senhaHash);

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        return new UsuarioResponseDTO(usuarioSalvo.getId(), usuarioSalvo.getNome(), usuarioSalvo.getEmail(), usuarioSalvo.getTipo(), usuarioSalvo.getStatus(), usuarioSalvo.getDataCriacao(), usuarioSalvo.getUltimoAcesso());
    }

    //instancia a subclasse certa para o JPA gravar tambem na tabela motoristas/responsaveis/administradores
    private Usuario criarPorTipo(TipoUsuario tipo){
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo de usuario e obrigatorio");
        }
        return switch (tipo) {
            case MOTORISTA -> {
                Motorista motorista = new Motorista();
                motorista.setStatusValidacao(StatusMotorista.PENDENTE);//so aparece na busca depois de validado
                yield motorista;
            }
            case RESPONSAVEL -> new Responsavel();
            case ADMINISTRADOR -> new Administrador();
        };
    }
}
