package br.com.smartvan.model;

import br.com.smartvan.enums.StatusMotorista;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Motorista extends Usuario{
    private Veiculo veiculo;
    private StatusMotorista statusMotorista;
}
