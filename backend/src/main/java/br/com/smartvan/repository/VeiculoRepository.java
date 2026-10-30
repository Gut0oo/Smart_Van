package br.com.smartvan.repository;

import br.com.smartvan.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByPlaca(String placa);

    List<Veiculo> findByMotoristaIdOrderByIdAsc(Long motoristaId);
}
