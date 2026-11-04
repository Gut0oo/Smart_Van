package br.com.smartvan.dto;

import br.com.smartvan.enums.TipoRota;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record GerarRotaRequestDTO(@NotNull LocalDate data, @NotNull TipoRota tipo, @NotNull Long atendimentoId,
                                  Long veiculoId, @JsonFormat(pattern = "HH:mm") LocalTime horarioSaida) {
}
