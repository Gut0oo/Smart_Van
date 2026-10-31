package br.com.smartvan.dto;

import br.com.smartvan.model.Atendimento;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/** Objeto {@code Atendimento} do contrato. */
public record AtendimentoResponseDTO(Long id, EscolaResponseDTO escola,
                                     @JsonFormat(pattern = "HH:mm") LocalTime horarioInicioAula,
                                     @JsonFormat(pattern = "HH:mm") LocalTime horarioSaidaMotorista,
                                     @JsonFormat(pattern = "HH:mm") LocalTime tetoChegada) {

    public static AtendimentoResponseDTO de(Atendimento a, int margemMinutos) {
        return new AtendimentoResponseDTO(a.getId(), EscolaResponseDTO.de(a.getEscola()), a.getHorarioInicioAula(),
                a.getHorarioSaidaMotorista(), a.tetoChegada(margemMinutos));
    }
}
