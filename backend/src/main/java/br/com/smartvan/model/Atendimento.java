package br.com.smartvan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

//turno que o motorista atende numa escola; um motorista pode ter vários por escola
@Getter
@Setter

@Entity
@Table(name = "atendimentos")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "atendimento_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escola_id", nullable = false)
    private Escola escola;

    @Column(name = "horario_inicio_aula", nullable = false)
    private LocalTime horarioInicioAula;

    @Column(name = "horario_saida_motorista", nullable = false)
    private LocalTime horarioSaidaMotorista;

    //horário limite de chegada à escola: início da aula menos a margem (smartvan.rota.margem-chegada-minutos)
    public LocalTime tetoChegada(int margemMinutos) {
        return horarioInicioAula.minusMinutes(margemMinutos);
    }
}
