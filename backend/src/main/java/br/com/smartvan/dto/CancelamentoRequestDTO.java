package br.com.smartvan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelamentoRequestDTO(@NotBlank @Size(min = 5, max = 500) String motivo) {
}
