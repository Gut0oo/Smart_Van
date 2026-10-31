package br.com.smartvan.dto;

import br.com.smartvan.model.Atendimento;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/** Atendimento no perfil do motorista. A lotação é só em números: nenhum nome de cliente sai daqui. */
public record AtendimentoLotacaoResponseDTO(Long id, EscolaResumoResponseDTO escola, String cidade,
                                            @JsonFormat(pattern = "HH:mm") LocalTime horarioInicioAula,
                                            @JsonFormat(pattern = "HH:mm") LocalTime horarioSaidaMotorista,
                                            long contratosAtivos, int capacidade, int vagasRestantes) {

    public static AtendimentoLotacaoResponseDTO de(Atendimento a, long contratosAtivos, int capacidade,
                                                   int vagasRestantes) {
        return new AtendimentoLotacaoResponseDTO(a.getId(), EscolaResumoResponseDTO.de(a.getEscola()),
                a.getEscola().getEndereco().getCidade(), a.getHorarioInicioAula(), a.getHorarioSaidaMotorista(),
                contratosAtivos, capacidade, vagasRestantes);
    }
}
