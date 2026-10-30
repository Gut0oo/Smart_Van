package br.com.smartvan.dto;

import br.com.smartvan.enums.TipoVeiculo;
import br.com.smartvan.model.Veiculo;

/** Maior veículo do motorista, como aparece na busca e no perfil. */
public record VeiculoResumoResponseDTO(String modelo, TipoVeiculo tipo, int capacidade) {

    public static VeiculoResumoResponseDTO de(Veiculo v) {
        return v == null ? null : new VeiculoResumoResponseDTO(v.getModelo(), v.getTipo(), v.getCapacidade());
    }
}
