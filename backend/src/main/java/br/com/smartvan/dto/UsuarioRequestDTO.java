package br.com.smartvan.dto;

import br.com.smartvan.enums.TipoUsuario;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequestDTO(String nome,
        String email,
        String senha,
        String cpf,
        String telefone,
        TipoUsuario tipo
) {
}
