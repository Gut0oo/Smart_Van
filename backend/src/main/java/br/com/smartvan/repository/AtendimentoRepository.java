package br.com.smartvan.repository;

import br.com.smartvan.model.Atendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    @Query("select a from Atendimento a join fetch a.escola e join fetch e.endereco "
            + "where a.motorista.id = :motoristaId "
            + "order by e.nome, e.id, a.horarioInicioAula, a.horarioSaidaMotorista, a.id")
    List<Atendimento> doMotorista(@Param("motoristaId") Long motoristaId);

    void deleteByMotoristaId(Long motoristaId);
}
