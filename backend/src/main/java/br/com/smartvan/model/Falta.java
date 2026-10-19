package br.com.smartvan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter

@Entity
@Table(name = "faltas",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "data_falta"}))//uma falta por aluno por dia
public class Falta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "falta_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    //dia em que o aluno nao vai (pode ser futuro), usado para tirar a parada da rota
    @Column(name = "data_falta", nullable = false)
    private LocalDate dataFalta;

    @Column(name = "motivo", columnDefinition = "text")
    private String motivo;

    @CreationTimestamp
    @Column(name = "data_registro", nullable = false, updatable = false)
    private Instant dataRegistro;
}
