package br.com.smartvan.repository;

import br.com.smartvan.model.Atendimento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    @Query("select a from Atendimento a join fetch a.escola e join fetch e.endereco "
            + "where a.motorista.id = :motoristaId "
            + "order by e.nome, e.id, a.horarioInicioAula, a.horarioSaidaMotorista, a.id")
    List<Atendimento> doMotorista(@Param("motoristaId") Long motoristaId);

    String BUSCA = "select a from Atendimento a join fetch a.motorista m join fetch a.escola e join fetch e.endereco en "
            + "where m.statusValidacao = br.com.smartvan.enums.StatusMotorista.VALIDADO "
            + "and a.horarioInicioAula between :inicioDe and :inicioAte "
            + "and lower(en.cidade) like :cidade and lower(m.nome) like :nome ";

    String ORDEM_DA_BUSCA = " order by m.nome, m.id, a.horarioInicioAula, a.horarioSaidaMotorista, a.id";

    //padrões já em minúsculas e com % (ex.: "%pinheiros%")
    @Query(BUSCA + "and e.id = :escolaId" + ORDEM_DA_BUSCA)
    List<Atendimento> buscar(@Param("escolaId") Long escolaId, @Param("inicioDe") LocalTime inicioDe,
                             @Param("inicioAte") LocalTime inicioAte, @Param("cidade") String cidade,
                             @Param("nome") String nome);

    //pesquisa de um motorista pelo nome, em qualquer escola
    @Query(BUSCA + ORDEM_DA_BUSCA)
    List<Atendimento> buscarEmTodasAsEscolas(@Param("inicioDe") LocalTime inicioDe,
                                             @Param("inicioAte") LocalTime inicioAte, @Param("cidade") String cidade,
                                             @Param("nome") String nome);

    //trava a linha antes de contar as vagas: propostas e aceites simultâneos do atendimento esperam um pelo outro
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Atendimento a where a.id = :id")
    Optional<Atendimento> travar(@Param("id") Long id);

    //mesma trava, para alterar a lista do motorista sem disputar com uma proposta em andamento
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Atendimento a where a.motorista.id = :motoristaId order by a.id")
    List<Atendimento> travarDoMotorista(@Param("motoristaId") Long motoristaId);
}
