package br.com.smartvan.dto;

import java.time.LocalDate;

/** {@code dataPagamento} é opcional: sem ela, vale a data de hoje. */
public record BaixaMensalidadeRequestDTO(LocalDate dataPagamento) {
}
