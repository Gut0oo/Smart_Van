package br.com.smartvan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "responsaveis")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Responsavel extends Usuario{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;
}
