package br.com.smartvan.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FaltaRequestDTO(@NotNull LocalDate data, String motivo) {
}
