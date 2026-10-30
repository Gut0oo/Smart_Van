package br.com.smartvan.dto;

import br.com.smartvan.model.Usuario;

/** Objeto {@code UsuarioResumo} do contrato. */
public record UsuarioResumoResponseDTO(Long id, String nome, String telefone) {

    public static UsuarioResumoResponseDTO de(Usuario u) {
        return new UsuarioResumoResponseDTO(u.getId(), u.getNome(), u.getTelefone());
    }
}
