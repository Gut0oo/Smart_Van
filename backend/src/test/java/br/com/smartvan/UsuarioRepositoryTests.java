package br.com.smartvan;

import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class UsuarioRepositoryTests {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void devePersistirUsuarioNoBancoDeTestes() {
        Usuario usuario = new Usuario();
        usuario.setNome("Gustavo");
        usuario.setEmail("gustavo@email.com");
        usuario.setSenha("senha-de-teste");
        usuario.setCpf("12345678900");
        usuario.setTelefone("11999999999");
        usuario.setTipo(TipoUsuario.RESPONSAVEL);
        usuario.setStatus(StatusConta.PENDENTE);

        Usuario usuarioSalvo = usuarioRepository.saveAndFlush(usuario);

        assertNotNull(usuarioSalvo.getId());
        assertEquals("gustavo@email.com", usuarioSalvo.getEmail());
    }
}
