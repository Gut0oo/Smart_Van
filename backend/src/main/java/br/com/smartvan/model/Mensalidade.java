package br.com.smartvan.model;

import br.com.smartvan.enums.StatusMensalidade;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter

@Entity
@Table(name = "mensalidades",
        uniqueConstraints = @UniqueConstraint(columnNames = {"contrato_id", "competencia"}))//evita gerar a mesma mensalidade duas vezes
public class Mensalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mensalidade_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private Contrato contrato;

    //mes de referencia, sempre salvo como dia 1 (ex: 2026-09-01)
    @Column(name = "competencia", nullable = false)
    private LocalDate competencia;

    @Column(name = "valor", precision = 10, scale = 2, nullable = false)
    private BigDecimal valor;

    @Column(name = "vencimento", nullable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private StatusMensalidade status;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;
}
