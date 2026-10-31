package br.com.smartvan.dto;

import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Veiculo;

import java.util.List;

/** Objeto {@code MotoristaBusca} do contrato. */
public record MotoristaBuscaResponseDTO(Long id, String nome, String telefone, VeiculoResumoResponseDTO veiculo,
                                        List<AtendimentoBuscaResponseDTO> atendimentos) {

    public static MotoristaBuscaResponseDTO de(Motorista m, Veiculo maior, List<AtendimentoBuscaResponseDTO> atendimentos) {
        return new MotoristaBuscaResponseDTO(m.getId(), m.getNome(), m.getTelefone(),
                VeiculoResumoResponseDTO.de(maior), atendimentos);
    }
}
