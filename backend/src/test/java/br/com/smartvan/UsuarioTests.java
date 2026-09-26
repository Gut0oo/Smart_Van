package br.com.smartvan;

import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTests {
    Usuario usuario = new Usuario();

    //testes para validação do emaill
    @Test
    void deveRetornarTrueParaEmailValido() {
        usuario.setEmail("gustavo@email.com");

        assertTrue(usuario.validarEmail());
    }

    @Test
    void deveRetornarFalseParaEmailNulo() {
        usuario.setEmail(null);

        assertFalse(usuario.validarEmail());
    }

    @Test
    void deveRetornarFalseParaEmailVazio() {
        usuario.setEmail("");

        assertFalse(usuario.validarEmail());
    }

    @Test
    void deveRetornarFalseParaEmailSemArroba() {
        usuario.setEmail("gustavoemail.com");

        assertFalse(usuario.validarEmail());
    }

    @Test
    void deveRetornarFalseParaEmailSemPonto() {
        usuario.setEmail("gustavo@emailcom");

        assertFalse(usuario.validarEmail());
    }


    //testes para setters
    @Test
    void deveAlterarNome() {
        usuario.setNome("Gustavo");
        assertEquals("Gustavo", usuario.getNome());
    }

    @Test
    void deveAlterarEmail() {
        usuario.setEmail("gustavo@email.com");
        assertEquals("gustavo@email.com", usuario.getEmail());
    }

    @Test
    void deveAlterarTipo(){
        usuario.setTipo(TipoUsuario.RESPONSAVEL);
        assertEquals(TipoUsuario.RESPONSAVEL, usuario.getTipo());
    }

    @Test
    void deveAlterarTelefone() {
        usuario.setTelefone("11999999999");
        assertEquals("11999999999", usuario.getTelefone());
    }

    @Test
    void deveAlterarCpf() {
        usuario.setCpf("12345678900");
        assertEquals("12345678900", usuario.getCpf());
    }

    //testes para ativação ou desativação da conta
    @Test
    void deveRetornarTrueQuandoUsuarioEstiverAtivo(){
        usuario.ativarConta();
        assertTrue(usuario.estaAtiva());
    }

    @Test
    void deveRetornarFalseQuandoUsuarioEstiverDesativo(){
        usuario.desativarConta();
        assertFalse(usuario.estaAtiva());
    }
}
