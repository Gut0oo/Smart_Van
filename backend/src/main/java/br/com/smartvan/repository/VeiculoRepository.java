package br.com.smartvan.repository;

import br.com.smartvan.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByPlaca(String placa);

    List<Veiculo> findByMotoristaIdOrderByIdAsc(Long motoristaId);

    //maior capacidade primeiro: o primeiro de cada motorista é o maior veículo dele
    @Query("select v from Veiculo v where v.motorista.id in :motoristaIds order by v.capacidade desc, v.id")
    List<Veiculo> dosMotoristasPorCapacidade(@Param("motoristaIds") Collection<Long> motoristaIds);

    @Query("select coalesce(max(v.capacidade), 0) from Veiculo v where v.motorista.id = :motoristaId")
    int maiorCapacidade(@Param("motoristaId") Long motoristaId);
}
