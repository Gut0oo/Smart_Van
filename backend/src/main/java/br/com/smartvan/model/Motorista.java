package br.com.smartvan.model;

import br.com.smartvan.enums.StatusMotorista;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

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

    //escolas atendidas pelo motorista (usado na busca por escola)
    @ManyToMany
    @JoinTable(
            name = "motoristas_escolas",
            joinColumns = @JoinColumn(name = "motorista_id"),
            inverseJoinColumns = @JoinColumn(name = "escola_id")
    )
    private Set<Escola> escolasAtendidas = new HashSet<>();
}
