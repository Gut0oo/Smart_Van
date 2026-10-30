package br.com.smartvan.repository;

import br.com.smartvan.model.Escola;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EscolaRepository extends JpaRepository<Escola, Long> {

    //padrões já em minúsculas e com % (ex.: "%pinheiros%")
    @Query("select e from Escola e join fetch e.endereco en "
            + "where lower(en.cidade) like :cidade and lower(e.nome) like :nome order by e.nome, e.id")
    List<Escola> buscar(@Param("cidade") String cidade, @Param("nome") String nome);
}
