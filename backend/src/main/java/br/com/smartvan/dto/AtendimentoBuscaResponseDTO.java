package br.com.smartvan.dto;

import br.com.smartvan.model.Atendimento;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/** Atendimento que combina com a busca, com as vagas dele. */
public record AtendimentoBuscaResponseDTO(Long id, EscolaResumoResponseDTO escola, String cidade,
                                          @JsonFormat(pattern = "HH:mm") LocalTime horarioInicioAula,
                                          @JsonFormat(pattern = "HH:mm") LocalTime horarioSaidaMotorista,
                                          int vagasRestantes) {

    public static AtendimentoBuscaResponseDTO de(Atendimento a, int vagasRestantes) {
        return new AtendimentoBuscaResponseDTO(a.getId(), EscolaResumoResponseDTO.de(a.getEscola()),
                a.getEscola().getEndereco().getCidade(), a.getHorarioInicioAula(), a.getHorarioSaidaMotorista(),
                vagasRestantes);
    }
}
