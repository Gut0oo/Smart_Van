package br.com.smartvan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EscolaRequestDTO(
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 20) String telefone,
        @NotNull @Valid EnderecoRequestDTO endereco) {
}
