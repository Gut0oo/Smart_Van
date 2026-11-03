package br.com.smartvan.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Sem {@code valorMensal}, a proposta é no piso do atendimento; com ele, vale qualquer valor positivo. */
public record PropostaRequestDTO(
        @NotNull Long atendimentoId,
        @NotNull Long alunoId,
        @Positive @Digits(integer = 8, fraction = 2) BigDecimal valorMensal) {
}
