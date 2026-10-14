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
    private Long veiculo_id;

    @Column(name = "placa")
    private String placa;
    @Column(name = "modelo")
    private String modelo;

    @Column(name = "ano")
    private int ano;

    @Column(name = "capacidade")
    private int capacidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoVeiculo tipo;

    public Veiculo(TipoVeiculo tipo, String placa, String modelo, int capacidade, int ano) {
        this.tipo = tipo;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidade = capacidade;
        this.ano = ano;
    }
}
