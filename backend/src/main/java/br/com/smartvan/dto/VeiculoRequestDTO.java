package br.com.smartvan.dto;

import br.com.smartvan.enums.TipoVeiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VeiculoRequestDTO(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{7}", message = "deve ter 7 letras/números, sem hífen") String placa,
        @NotBlank @Size(max = 100) String modelo,
        @NotNull @Min(1950) @Max(2100) Integer ano,
        @NotNull @Min(1) @Max(60) Integer capacidade,
        @NotNull TipoVeiculo tipo) {
}
