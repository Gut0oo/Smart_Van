package br.com.smartvan.repository;

import br.com.smartvan.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByEntidadeAndEntidadeIdOrderByDataRegistroAscIdAsc(String entidade, Long entidadeId);
}
