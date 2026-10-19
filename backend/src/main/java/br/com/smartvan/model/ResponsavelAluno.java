package br.com.smartvan.model;

import br.com.smartvan.enums.Parentesco;
import br.com.smartvan.enums.StatusVinculo;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter

@Entity
@Table(name = "responsaveis_alunos")
public class ResponsavelAluno {

    //chave composta (responsavel_id, aluno_id)
    @EmbeddedId
    private ResponsavelAlunoId id = new ResponsavelAlunoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("responsavelId")
    @JoinColumn(name = "responsavel_id")
    private Responsavel responsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("alunoId")
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @Enumerated(EnumType.STRING)
    @Column(name = "parentesco", length = 30, nullable = false)
    private Parentesco parentesco;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_vinculo", length = 30, nullable = false)
    private StatusVinculo statusVinculo;

    //se pode ver a localizacao da van em tempo real (REQ08)
    @Column(name = "acompanha_rota", nullable = false)
    private boolean acompanhaRota;

    @Getter
    @Setter
    @EqualsAndHashCode
    @Embeddable
    public static class ResponsavelAlunoId implements Serializable {

        @Column(name = "responsavel_id")
        private Long responsavelId;

        @Column(name = "aluno_id")
        private Long alunoId;
    }
}
