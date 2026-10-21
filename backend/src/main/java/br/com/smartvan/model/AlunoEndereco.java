package br.com.smartvan.model;

import br.com.smartvan.enums.TipoEndereco;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter

@Entity
@Table(name = "alunos_enderecos")
public class AlunoEndereco {

    //chave composta (aluno_id, endereco_id)
    @EmbeddedId
    private AlunoEnderecoId id = new AlunoEnderecoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("alunoId")
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("enderecoId")
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoEndereco tipo;

    //endereco usado por padrao na rota
    @Column(name = "principal", nullable = false)
    private boolean principal;

    @Getter
    @Setter
    @EqualsAndHashCode
    @Embeddable
    public static class AlunoEnderecoId implements Serializable {

        @Column(name = "aluno_id")
        private Long alunoId;

        @Column(name = "endereco_id")
        private Long enderecoId;
    }
}
