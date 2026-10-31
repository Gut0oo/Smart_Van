package br.com.smartvan.dto;

import br.com.smartvan.model.Aluno;

/** Objeto {@code AlunoResumo} do contrato. */
public record AlunoResumoResponseDTO(Long id, String nome) {

    public static AlunoResumoResponseDTO de(Aluno a) {
        return new AlunoResumoResponseDTO(a.getId(), a.getNome());
    }
}
