package br.com.smartvan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Corpo padrão de todas as respostas de erro (seção "Erros" do docs/CONTRATO_API.md).
 * {@code campos} só aparece nos erros de validação.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponseDTO(int status, String codigo, String mensagem, Map<String, String> campos,
                              Instant timestamp) {

    public static ErroResponseDTO de(int status, String codigo, String mensagem, Instant timestamp) {
        return new ErroResponseDTO(status, codigo, mensagem, null, timestamp);
    }
}
