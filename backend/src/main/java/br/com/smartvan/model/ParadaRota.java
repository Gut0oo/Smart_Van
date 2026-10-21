package br.com.smartvan.model;

import br.com.smartvan.enums.TipoParada;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter

@Entity
@Table(name = "paradas_rota",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rota_id", "ordem"}))
public class ParadaRota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parada_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rota_id", nullable = false)
    private Rota rota;

    //nulo quando a parada é a escola (desembarque de varios alunos)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endereco_id", nullable = false)
    private Endereco endereco;

    @Column(name = "ordem", nullable = false)
    private int ordem;

    @Column(name = "horario_previsto")
    private LocalTime horarioPrevisto;
    @Column(name = "horario_realizado")
    private LocalTime horarioRealizado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_parada", length = 30, nullable = false)
    private TipoParada tipoParada;
}
