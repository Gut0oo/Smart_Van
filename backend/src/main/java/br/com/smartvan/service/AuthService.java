package br.com.smartvan.service;

import br.com.smartvan.dto.LoginRequestDTO;
import br.com.smartvan.dto.LoginResponseDTO;
import br.com.smartvan.dto.UsuarioDetalheResponseDTO;
import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final Clock clock;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder encoder, Clock clock) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.clock = clock;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO req) {
        Usuario usuario = usuarios.findByEmailIgnoreCaseOrderById(req.email().trim()).stream()
                .filter(u -> encoder.matches(req.senha(), u.getSenha()))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS",
                        "E-mail ou senha inválidos."));
        //só depois de conferir a senha, para não revelar a situação da conta a quem não a conhece
        if (usuario.getStatus() == StatusConta.BLOQUEADA) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN, "CONTA_BLOQUEADA", "Sua conta está bloqueada.");
        }
        usuario.setUltimoAcesso(clock.instant());
        return new LoginResponseDTO(UsuarioDetalheResponseDTO.de(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDetalheResponseDTO me(Long usuarioId) {
        return usuarios.findById(usuarioId)
                .map(UsuarioDetalheResponseDTO::de)
                .orElseThrow(() -> new RegraNegocioException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                        "Faça login para continuar."));
    }
}
