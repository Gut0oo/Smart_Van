package br.com.smartvan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Corpo de PUT /motorista/escolas: a lista substitui todos os atendimentos do motorista. */
public record AtendimentosRequestDTO(@NotNull List<@NotNull @Valid AtendimentoRequestDTO> atendimentos) {
}
