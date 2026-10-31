package br.com.smartvan.repository;

import br.com.smartvan.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    //lista porque o cadastro guarda o e-mail como veio: a unicidade do banco diferencia maiúsculas de minúsculas
    List<Usuario> findByEmailIgnoreCaseOrderById(String email);
}
