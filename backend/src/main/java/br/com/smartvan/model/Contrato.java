package br.com.smartvan.model;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.TipoUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter

@Entity
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contrato_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Responsavel responsavel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    //cada contrato e de um unico aluno
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    //turno contratado; fica nulo só em contrato RECUSADO ou ENCERRADO cujo atendimento o motorista removeu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendimento_id")
    private Atendimento atendimento;

    @Column(name = "valor_mensal", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorMensal;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    //PENDENTE = proposta enviada e aguardando o motorista
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private StatusContrato status;

    //cancelamento com aviso previo de 30 dias
    @Column(name = "data_solicitacao_cancelamento")
    private LocalDate dataSolicitacaoCancelamento;

    @Column(name = "data_encerramento_efetivo")
    private LocalDate dataEncerramentoEfetivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "solicitante_cancelamento", length = 30)
    private TipoUsuario solicitanteCancelamento;

    @Column(name = "motivo_cancelamento", columnDefinition = "text")
    private String motivoCancelamento;
}
