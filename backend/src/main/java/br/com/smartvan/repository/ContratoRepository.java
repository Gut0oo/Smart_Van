package br.com.smartvan.repository;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.model.Contrato;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    String COM_ASSOCIACOES = "select c from Contrato c join fetch c.responsavel r join fetch c.motorista m "
            + "join fetch c.aluno al left join fetch al.escola "
            + "left join fetch c.atendimento atend left join fetch atend.escola ";

    String MAIS_RECENTES_PRIMEIRO = " order by c.dataInicio desc, c.id desc";

    @Query(COM_ASSOCIACOES + "where c.id = :id")
    Optional<Contrato> carregar(@Param("id") Long id);

    //trava a linha do contrato: aceite e recusa simultâneos do mesmo contrato esperam um pelo outro
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contrato c where c.id = :id")
    Optional<Contrato> travar(@Param("id") Long id);

    @Query(COM_ASSOCIACOES + "where r.id = :usuarioId or m.id = :usuarioId" + MAIS_RECENTES_PRIMEIRO)
    List<Contrato> doUsuario(@Param("usuarioId") Long usuarioId);

    @Query(COM_ASSOCIACOES + "where (r.id = :usuarioId or m.id = :usuarioId) and c.status = :status"
            + MAIS_RECENTES_PRIMEIRO)
    List<Contrato> doUsuarioComStatus(@Param("usuarioId") Long usuarioId, @Param("status") StatusContrato status);

    long countByAtendimentoIdAndStatus(Long atendimentoId, StatusContrato status);

    boolean existsByAlunoIdAndMotoristaIdAndStatus(Long alunoId, Long motoristaId, StatusContrato status);

    boolean existsByAlunoIdAndStatus(Long alunoId, StatusContrato status);

    boolean existsByAtendimentoIdInAndStatusIn(Collection<Long> atendimentoIds, Collection<StatusContrato> status);

    //cada linha: [atendimentoId, quantidade]; atendimento sem contrato ATIVO não vem
    @Query("select c.atendimento.id, count(c) from Contrato c "
            + "where c.atendimento.id in :atendimentoIds "
            + "and c.status = br.com.smartvan.enums.StatusContrato.ATIVO group by c.atendimento.id")
    List<Object[]> contarAtivosPorAtendimento(@Param("atendimentoIds") Collection<Long> atendimentoIds);

    //responsáveis distintos que já tiveram contrato com o motorista; proposta PENDENTE ou RECUSADO não conta
    @Query("select count(distinct c.responsavel.id) from Contrato c where c.motorista.id = :motoristaId "
            + "and c.status in (br.com.smartvan.enums.StatusContrato.ATIVO, "
            + "br.com.smartvan.enums.StatusContrato.ENCERRADO)")
    long contarClientes(@Param("motoristaId") Long motoristaId);

    //o histórico continua, sem o atendimento que está sendo removido
    @Modifying
    @Query("update Contrato c set c.atendimento = null where c.atendimento.id in :atendimentoIds")
    void desvincularAtendimentos(@Param("atendimentoIds") Collection<Long> atendimentoIds);
}
