package br.com.smartvan.repository;

import br.com.smartvan.enums.StatusVinculo;
import br.com.smartvan.model.ResponsavelAluno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponsavelAlunoRepository
        extends JpaRepository<ResponsavelAluno, ResponsavelAluno.ResponsavelAlunoId> {

    @Query("select ra from ResponsavelAluno ra join fetch ra.aluno a left join fetch a.escola "
            + "where ra.responsavel.id = :responsavelId "
            + "and ra.statusVinculo = br.com.smartvan.enums.StatusVinculo.ATIVO order by a.nome, a.id")
    List<ResponsavelAluno> ativosDoResponsavel(@Param("responsavelId") Long responsavelId);

    boolean existsByResponsavelIdAndStatusVinculo(Long responsavelId, StatusVinculo statusVinculo);

    boolean existsByResponsavelIdAndAlunoIdAndStatusVinculo(Long responsavelId, Long alunoId,
                                                            StatusVinculo statusVinculo);
}
