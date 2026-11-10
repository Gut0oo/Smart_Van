package br.com.smartvan.dto;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Endereco;
import br.com.smartvan.model.Escola;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Objeto {@code Contrato} do contrato da API. As associações são lazy: chame {@link #de} dentro de uma transação.
 * {@code atendimento} sai nulo se o motorista removeu o turno de um contrato já terminado, e {@code cancelamento}
 * só vem preenchido quando há pedido de cancelamento. {@code enderecoAluno} é o endereço principal do aluno.
 */
public record ContratoResponseDTO(Long id, StatusContrato status, BigDecimal valorMensal, LocalDate dataInicio,
                                  AlunoResumoResponseDTO aluno, UsuarioResumoResponseDTO responsavel,
                                  UsuarioResumoResponseDTO motorista, EscolaResumoResponseDTO escola,
                                  AtendimentoResumo atendimento, Cancelamento cancelamento,
                                  EnderecoResponseDTO enderecoAluno) {

    public record AtendimentoResumo(Long id,
                                    @JsonFormat(pattern = "HH:mm") LocalTime horarioInicioAula,
                                    @JsonFormat(pattern = "HH:mm") LocalTime horarioSaidaMotorista,
                                    BigDecimal valorMensal) {
    }

    public record Cancelamento(LocalDate dataSolicitacao, LocalDate dataEncerramentoEfetivo,
                               TipoUsuario solicitante, String motivo) {
    }

    public static ContratoResponseDTO de(Contrato c) {
        return de(c, null);
    }

    public static ContratoResponseDTO de(Contrato c, Endereco enderecoAluno) {
        Atendimento a = c.getAtendimento();
        Escola escola = a != null ? a.getEscola() : c.getAluno().getEscola();
        //o contrato recém-criado ainda tem o valor como veio no pedido; o da API tem sempre 2 casas
        return new ContratoResponseDTO(c.getId(), c.getStatus(), Dinheiro.duasCasas(c.getValorMensal()), c.getDataInicio(),
                AlunoResumoResponseDTO.de(c.getAluno()), UsuarioResumoResponseDTO.de(c.getResponsavel()),
                UsuarioResumoResponseDTO.de(c.getMotorista()),
                escola == null ? null : EscolaResumoResponseDTO.de(escola),
                a == null ? null : new AtendimentoResumo(a.getId(), a.getHorarioInicioAula(), a.getHorarioSaidaMotorista(),
                        Dinheiro.duasCasas(a.getValorMensal())),
                c.getDataSolicitacaoCancelamento() == null ? null
                        : new Cancelamento(c.getDataSolicitacaoCancelamento(), c.getDataEncerramentoEfetivo(),
                                c.getSolicitanteCancelamento(), c.getMotivoCancelamento()),
                enderecoAluno == null ? null : EnderecoResponseDTO.de(enderecoAluno));
    }
}
