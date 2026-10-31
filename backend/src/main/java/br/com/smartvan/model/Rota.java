package br.com.smartvan.model;

import br.com.smartvan.enums.StatusRota;
import br.com.smartvan.enums.TipoRota;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter

@Entity
@Table(name = "rotas")
public class Rota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rota_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    //uma rota por escola: na ida e o destino, na volta e o ponto de saida
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escola_id", nullable = false)
    private Escola escola;

    @Column(name = "data_rota", nullable = false)
    private LocalDate dataRota;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoRota tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private StatusRota status;

    @Column(name = "horario_inicio")
    private LocalTime horarioInicio;
    @Column(name = "horario_fim")
    private LocalTime horarioFim;

    @Column(name = "tempo_estimado_minutos")
    private Integer tempoEstimadoMinutos;

    @Column(name = "distancia_km", precision = 10, scale = 2)
    private BigDecimal distanciaKm;

    //false quando o Google falhou e a ordem foi calculada por proximidade
    @Column(name = "otimizada", nullable = false)
    private boolean otimizada;

    //tracado da rota devolvido pelo Google (formato "encoded polyline"); nulo quando nao otimizada
    @Column(name = "polyline", columnDefinition = "text")
    private String polyline;

    //a rota é dona das paradas: salvar/remover a rota salva/remove as paradas
    @OneToMany(mappedBy = "rota", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ParadaRota> paradas = new ArrayList<>();

    public void adicionarParada(ParadaRota parada) {
        parada.setRota(this);
        this.paradas.add(parada);
    }
}
