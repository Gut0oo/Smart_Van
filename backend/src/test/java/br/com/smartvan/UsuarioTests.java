package br.com.smartvan;

import br.com.smartvan.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class UsuarioTests {

    @Test
    void deveAlterarNome() {
        Usuario usuario = new Usuario();

        usuario.setNome("Gustavo");

        assertEquals("Gustavo", usuario.getNome());
    }

    @Test
    void deveAlterarEmail() {
        Usuario usuario = new Usuario();

        usuario.setEmail("gustavo@email.com");

        assertEquals("gustavo@email.com", usuario.getEmail());
    }
}
