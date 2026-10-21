package br.com.smartvan.model;

import br.com.smartvan.enums.StatusMotorista;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter

@Entity
@Table(name = "validacoes")
public class Validacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "validacao_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrador_id", nullable = false)
    private Administrador administrador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @CreationTimestamp
    @Column(name = "data_validacao", nullable = false, updatable = false)
    private Instant dataValidacao;

    //VALIDADO ou REJEITADO
    @Enumerated(EnumType.STRING)
    @Column(name = "decisao", length = 30, nullable = false)
    private StatusMotorista decisao;

    //obrigatorio quando rejeitado
    @Column(name = "parecer", columnDefinition = "text")
    private String parecer;
}
