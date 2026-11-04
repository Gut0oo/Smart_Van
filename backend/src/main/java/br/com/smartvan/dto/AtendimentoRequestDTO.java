package br.com.smartvan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

import java.time.LocalTime;

public record AtendimentoRequestDTO(
        @NotNull Long escolaId,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horarioInicioAula,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horarioSaidaMotorista,
        @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horarioSaidaEscola,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal valorMensal) {
}
