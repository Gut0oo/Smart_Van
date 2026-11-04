package br.com.smartvan.dto;

import br.com.smartvan.model.Falta;

import java.time.LocalDate;

/** Objeto {@code Falta} do contrato. */
public record FaltaResponseDTO(Long id, LocalDate data, String motivo) {

    public static FaltaResponseDTO de(Falta f) {
        return new FaltaResponseDTO(f.getId(), f.getDataFalta(), f.getMotivo());
    }
}
