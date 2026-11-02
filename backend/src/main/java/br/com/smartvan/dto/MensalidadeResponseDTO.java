package br.com.smartvan.dto;

import br.com.smartvan.enums.StatusMensalidade;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Mensalidade;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Objeto {@code Mensalidade} do contrato da API. */
public record MensalidadeResponseDTO(Long id, Long contratoId, LocalDate competencia, BigDecimal valor,
                                     LocalDate vencimento, StatusMensalidade status, LocalDate dataPagamento,
                                     AlunoResumoResponseDTO aluno, UsuarioResumoResponseDTO responsavel,
                                     UsuarioResumoResponseDTO motorista) {

    public static MensalidadeResponseDTO de(Mensalidade m) {
        Contrato c = m.getContrato();
        return new MensalidadeResponseDTO(m.getId(), c.getId(), m.getCompetencia(), m.getValor(), m.getVencimento(),
                m.getStatus(), m.getDataPagamento(), AlunoResumoResponseDTO.de(c.getAluno()),
                UsuarioResumoResponseDTO.de(c.getResponsavel()), UsuarioResumoResponseDTO.de(c.getMotorista()));
    }
}
