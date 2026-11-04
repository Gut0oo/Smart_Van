package br.com.smartvan.repository;

import br.com.smartvan.enums.StatusRota;
import br.com.smartvan.enums.TipoRota;
import br.com.smartvan.model.Rota;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RotaRepository extends JpaRepository<Rota, Long> {

    //os grafos trazem tudo que o RotaResponseDTO lê numa consulta só, sem N+1 por parada

    @EntityGraph(attributePaths = {"atendimento", "escola", "veiculo", "paradas", "paradas.aluno", "paradas.endereco"})
    List<Rota> findByMotoristaIdAndDataRotaAndTipoAndAtendimentoIdAndStatus(
            Long motoristaId, LocalDate dataRota, TipoRota tipo, Long atendimentoId, StatusRota status);

    @EntityGraph(attributePaths = {"atendimento", "escola", "veiculo", "paradas", "paradas.aluno", "paradas.endereco"})
    List<Rota> findByMotoristaIdAndDataRotaOrderByHorarioInicioAscIdAsc(Long motoristaId, LocalDate dataRota);

    @EntityGraph(attributePaths = {"atendimento", "escola", "veiculo", "paradas", "paradas.aluno", "paradas.endereco"})
    @Query("select r from Rota r where r.id = :id")
    Optional<Rota> carregar(@Param("id") Long id);

    //a rota continua no histórico, sem o atendimento que está sendo removido
    @Modifying
    @Query("update Rota r set r.atendimento = null where r.atendimento.id in :atendimentoIds")
    void desvincularAtendimentos(@Param("atendimentoIds") Collection<Long> atendimentoIds);
}
