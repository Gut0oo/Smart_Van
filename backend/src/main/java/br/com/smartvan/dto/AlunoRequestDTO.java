package br.com.smartvan.dto;

import br.com.smartvan.enums.Parentesco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AlunoRequestDTO(
        @NotBlank @Size(max = 255) String nome,
        @NotNull @PastOrPresent LocalDate dataNascimento,
        @Size(max = 30) String serie,
        @NotNull Long escolaId,
        @NotNull Parentesco parentesco,
        @NotNull @Valid EnderecoRequestDTO endereco) {
}
