package br.com.smartvan.dto;

import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Veiculo;

import java.util.List;

/** Objeto {@code MotoristaPerfil} do contrato: o que o responsável vê em {@code GET /motoristas/{id}}. */
public record MotoristaPerfilPublicoResponseDTO(Long id, String nome, String telefone, VeiculoResumoResponseDTO veiculo,
                                                List<AtendimentoLotacaoResponseDTO> atendimentos, long totalClientes) {

    public static MotoristaPerfilPublicoResponseDTO de(Motorista m, Veiculo maior,
                                                       List<AtendimentoLotacaoResponseDTO> atendimentos,
                                                       long totalClientes) {
        return new MotoristaPerfilPublicoResponseDTO(m.getId(), m.getNome(), m.getTelefone(),
                VeiculoResumoResponseDTO.de(maior), atendimentos, totalClientes);
    }
}
