package br.com.smartvan.model;

import br.com.smartvan.enums.StatusMotorista;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "motoristas")
public class Motorista extends Usuario{

    @OneToOne
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_validacao", nullable = false)
    private StatusMotorista statusMotorista;
}
