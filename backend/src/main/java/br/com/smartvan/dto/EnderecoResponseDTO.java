package br.com.smartvan.dto;

import br.com.smartvan.model.Endereco;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/** Objeto {@code EnderecoResp} do contrato. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EnderecoResponseDTO(Long id, String logradouro, String numero, String complemento, String bairro,
                                  String cidade, String uf, String cep, BigDecimal latitude, BigDecimal longitude) {

    public static EnderecoResponseDTO de(Endereco e) {
        return new EnderecoResponseDTO(e.getId(), e.getLogradouro(), e.getNumero(), e.getComplemento(), e.getBairro(),
                e.getCidade(), e.getUf(), e.getCep(), e.getLatitude(), e.getLongitude());
    }
}
