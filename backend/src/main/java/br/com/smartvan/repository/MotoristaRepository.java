package br.com.smartvan.repository;

import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.model.Motorista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MotoristaRepository extends JpaRepository<Motorista, Long> {

    List<Motorista> findByStatusValidacaoInOrderByIdAsc(Collection<StatusMotorista> status);
}
