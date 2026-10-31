package br.com.smartvan.dto;

import br.com.smartvan.model.Escola;

/** Objeto {@code EscolaResumo} do contrato. */
public record EscolaResumoResponseDTO(Long id, String nome) {

    public static EscolaResumoResponseDTO de(Escola e) {
        return new EscolaResumoResponseDTO(e.getId(), e.getNome());
    }
}
