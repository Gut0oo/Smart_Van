package br.com.smartvan;

import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UsuarioTests {

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

    @Test
    void deveAlterarTipo(){
        Usuario usuario = new Usuario();

        usuario.setTipo(TipoUsuario.RESPONSAVEL);

        assertEquals(TipoUsuario.RESPONSAVEL, usuario.getTipo());
    }

    @Test
    void deveAlterarTelefone() {
        Usuario usuario = new Usuario();

        usuario.setTelefone("11999999999");

        assertEquals("11999999999", usuario.getTelefone());
    }

    @Test
    void deveAlterarCpf() {
        Usuario usuario = new Usuario();

        usuario.setCpf("12345678900");

        assertEquals("12345678900", usuario.getCpf());
    }
}
