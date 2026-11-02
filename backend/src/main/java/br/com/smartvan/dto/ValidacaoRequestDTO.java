package br.com.smartvan.dto;

import br.com.smartvan.enums.StatusMotorista;
import jakarta.validation.constraints.NotNull;

/** {@code decisao}: VALIDADO ou REJEITADO. {@code parecer} é obrigatório ao rejeitar. */
public record ValidacaoRequestDTO(@NotNull StatusMotorista decisao, String parecer) {
}
