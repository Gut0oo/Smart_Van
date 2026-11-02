package br.com.smartvan.repository;

import br.com.smartvan.model.Validacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ValidacaoRepository extends JpaRepository<Validacao, Long> {
}
