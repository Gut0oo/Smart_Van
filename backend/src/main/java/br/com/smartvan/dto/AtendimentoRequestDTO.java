package br.com.smartvan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AtendimentoRequestDTO(
        @NotNull Long escolaId,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horarioInicioAula,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horarioSaidaMotorista) {
}
