package br.com.smartvan.dto;

import br.com.smartvan.model.Escola;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Objeto {@code Escola} do contrato. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EscolaResponseDTO(Long id, String nome, String telefone, EnderecoResponseDTO endereco) {

    public static EscolaResponseDTO de(Escola e) {
        return new EscolaResponseDTO(e.getId(), e.getNome(), e.getTelefone(), EnderecoResponseDTO.de(e.getEndereco()));
    }
}
