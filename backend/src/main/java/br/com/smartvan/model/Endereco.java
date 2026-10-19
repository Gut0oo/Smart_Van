package br.com.smartvan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter

@Entity
@Table(name = "enderecos")
public class Endereco{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "endereco_id")
    private Long id;

    @Column(name = "logradouro", nullable = false)
    private String logradouro;
    @Column(name = "numero", length = 20, nullable = false)
    private String numero;
    @Column(name = "complemento", length = 100)
    private String complemento;

    @Column(name = "bairro", length = 100, nullable = false)
    private String bairro;
    @Column(name = "cidade", length = 100, nullable = false)
    private String cidade;
    @Column(name = "uf", length = 2, nullable = false)
    private String uf;
    @Column(name = "cep", length = 8, nullable = false)
    private String cep;

    //preenchidos depois via serviço de mapas (geocodificação)
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;
}
