package br.com.smartvan.dto;

import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.enums.TipoUsuario;

import java.time.Instant;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        TipoUsuario tipo,
        StatusConta status,
        Instant dataCriacao,
        Instant ultimoAcesso
) {
}
