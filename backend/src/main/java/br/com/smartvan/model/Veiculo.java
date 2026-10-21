package br.com.smartvan.model;

import br.com.smartvan.enums.TipoVeiculo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "veiculos")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "veiculo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @Column(name = "placa", length = 7, unique = true, nullable = false)
    private String placa;
    @Column(name = "modelo", length = 100, nullable = false)
    private String modelo;

    @Column(name = "ano", nullable = false)
    private int ano;

    @Column(name = "capacidade", nullable = false)
    private int capacidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoVeiculo tipo;

    public Veiculo(){}//obrigatorio para o JPA

    public Veiculo(Motorista motorista, TipoVeiculo tipo, String placa, String modelo, int capacidade, int ano) {
        this.motorista = motorista;
        this.tipo = tipo;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidade = capacidade;
        this.ano = ano;
    }
}
