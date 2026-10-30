package br.com.smartvan.model;

import br.com.smartvan.enums.StatusMotorista;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "motoristas")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Motorista extends Usuario{

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao", length = 30, nullable = false)
    private StatusMotorista statusValidacao;

    //de onde a van sai na ida e para onde volta no fim da volta (casa ou garagem)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_partida_id")
    private Endereco enderecoPartida;
}
