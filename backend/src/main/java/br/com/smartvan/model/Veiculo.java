package br.com.smartvan.model;

import br.com.smartvan.enums.TipoVeiculo;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Veiculo {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String placa;
    private String modelo;

    private int ano;
    private int capacidade;

    private TipoVeiculo tipo;

    public Veiculo(TipoVeiculo tipo, String placa, String modelo, int capacidade, int ano) {
        this.tipo = tipo;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidade = capacidade;
        this.ano = ano;
    }

    @Override
    public String toString() {
        return "Veiculo{" +
                "ano=" + ano +
                ", placa='" + placa + '\'' +
                ", modelo='" + modelo + '\'' +
                ", capacidade=" + capacidade +
                ", tipo=" + tipo +
                '}';
    }
}
