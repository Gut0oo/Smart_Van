package br.com.smartvan.dto;

import br.com.smartvan.enums.TipoVeiculo;
import br.com.smartvan.model.Veiculo;

/** Objeto {@code Veiculo} do contrato. */
public record VeiculoResponseDTO(Long id, String placa, String modelo, int ano, int capacidade, TipoVeiculo tipo) {

    public static VeiculoResponseDTO de(Veiculo v) {
        return new VeiculoResponseDTO(v.getId(), v.getPlaca(), v.getModelo(), v.getAno(), v.getCapacidade(),
                v.getTipo());
    }
}
