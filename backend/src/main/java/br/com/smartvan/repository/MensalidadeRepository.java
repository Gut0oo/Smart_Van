package br.com.smartvan.repository;

import br.com.smartvan.enums.StatusMensalidade;
import br.com.smartvan.model.Mensalidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Os filtros opcionais são uma consulta por combinação (como em ContratoRepository), sem parâmetro nulo nem flag:
 * o SQL gerado é o mesmo no H2 dos testes e no PostgreSQL de produção.
 */
@Repository
public interface MensalidadeRepository extends JpaRepository<Mensalidade, Long> {

    String COM_ASSOCIACOES = "select m from Mensalidade m join fetch m.contrato c join fetch c.responsavel r "
            + "join fetch c.motorista mot join fetch c.aluno ";

    String COMPETENCIA_DESC = " order by m.competencia desc, m.id desc";

    @Query(COM_ASSOCIACOES + "where c.id = :contratoId" + COMPETENCIA_DESC)
    List<Mensalidade> doContrato(@Param("contratoId") Long contratoId);

    @Query(COM_ASSOCIACOES + "where r.id = :responsavelId" + COMPETENCIA_DESC)
    List<Mensalidade> doResponsavel(@Param("responsavelId") Long responsavelId);

    @Query(COM_ASSOCIACOES + "where r.id = :responsavelId and m.status = :status" + COMPETENCIA_DESC)
    List<Mensalidade> doResponsavelComStatus(@Param("responsavelId") Long responsavelId,
                                             @Param("status") StatusMensalidade status);

    @Query(COM_ASSOCIACOES + "where mot.id = :motoristaId" + COMPETENCIA_DESC)
    List<Mensalidade> doMotorista(@Param("motoristaId") Long motoristaId);

    @Query(COM_ASSOCIACOES + "where mot.id = :motoristaId and m.status = :status" + COMPETENCIA_DESC)
    List<Mensalidade> doMotoristaComStatus(@Param("motoristaId") Long motoristaId,
                                           @Param("status") StatusMensalidade status);

    @Query(COM_ASSOCIACOES + "where mot.id = :motoristaId and m.competencia = :competencia" + COMPETENCIA_DESC)
    List<Mensalidade> doMotoristaComCompetencia(@Param("motoristaId") Long motoristaId,
                                                @Param("competencia") LocalDate competencia);

    @Query(COM_ASSOCIACOES + "where mot.id = :motoristaId and m.status = :status and m.competencia = :competencia"
            + COMPETENCIA_DESC)
    List<Mensalidade> doMotoristaComStatusECompetencia(@Param("motoristaId") Long motoristaId,
                                                       @Param("status") StatusMensalidade status,
                                                       @Param("competencia") LocalDate competencia);

    //trava a linha: baixa e marcação de atraso simultâneas esperam uma pela outra e a baixa não se perde
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mensalidade m where m.id = :id")
    Optional<Mensalidade> travar(@Param("id") Long id);

    @Query("select m.id from Mensalidade m where m.status = br.com.smartvan.enums.StatusMensalidade.PENDENTE "
            + "and m.vencimento < :hoje order by m.id")
    List<Long> idsPendentesVencidas(@Param("hoje") LocalDate hoje);

    boolean existsByContratoIdAndCompetencia(Long contratoId, LocalDate competencia);

    boolean existsByContratoAlunoIdAndCompetencia(Long alunoId, LocalDate competencia);

    boolean existsByContratoIdAndStatusIn(Long contratoId, Collection<StatusMensalidade> status);
}
