package br.com.smartvan.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PropostaRequestDTO(
        @NotNull Long atendimentoId,
        @NotNull Long alunoId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal valorMensal) {
}
