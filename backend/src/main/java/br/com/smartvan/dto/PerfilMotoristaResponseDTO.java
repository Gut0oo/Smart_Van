package br.com.smartvan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PerfilMotoristaResponseDTO(UsuarioDetalheResponseDTO usuario, EnderecoResponseDTO enderecoPartida,
                                         List<VeiculoResponseDTO> veiculos, List<EscolaResumoResponseDTO> escolas) {
}
