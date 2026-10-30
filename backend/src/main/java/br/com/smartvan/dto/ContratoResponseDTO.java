package br.com.smartvan.dto;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Escola;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Objeto {@code Contrato} do contrato da API. As associações são lazy: chame {@link #de} dentro de uma transação.
 * {@code atendimento} sai nulo se o motorista removeu o turno de um contrato já terminado, e {@code cancelamento}
 * só vem preenchido quando há pedido de cancelamento.
 */
public record ContratoResponseDTO(Long id, StatusContrato status, BigDecimal valorMensal, LocalDate dataInicio,
                                  AlunoResumoResponseDTO aluno, UsuarioResumoResponseDTO responsavel,
                                  UsuarioResumoResponseDTO motorista, EscolaResumoResponseDTO escola,
                                  AtendimentoResumo atendimento, Cancelamento cancelamento) {

    public record AtendimentoResumo(Long id,
                                    @JsonFormat(pattern = "HH:mm") LocalTime horarioInicioAula,
                                    @JsonFormat(pattern = "HH:mm") LocalTime horarioSaidaMotorista) {
    }

    public record Cancelamento(LocalDate dataSolicitacao, LocalDate dataEncerramentoEfetivo,
                               TipoUsuario solicitante, String motivo) {
    }

    public static ContratoResponseDTO de(Contrato c) {
        Atendimento a = c.getAtendimento();
        Escola escola = a != null ? a.getEscola() : c.getAluno().getEscola();
        return new ContratoResponseDTO(c.getId(), c.getStatus(), c.getValorMensal(), c.getDataInicio(),
                AlunoResumoResponseDTO.de(c.getAluno()), UsuarioResumoResponseDTO.de(c.getResponsavel()),
                UsuarioResumoResponseDTO.de(c.getMotorista()),
                escola == null ? null : EscolaResumoResponseDTO.de(escola),
                a == null ? null : new AtendimentoResumo(a.getId(), a.getHorarioInicioAula(), a.getHorarioSaidaMotorista()),
                c.getDataSolicitacaoCancelamento() == null ? null
                        : new Cancelamento(c.getDataSolicitacaoCancelamento(), c.getDataEncerramentoEfetivo(),
                                c.getSolicitanteCancelamento(), c.getMotivoCancelamento()));
    }
}
