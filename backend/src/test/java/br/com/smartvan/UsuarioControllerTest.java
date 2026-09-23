package br.com.smartvan;


import br.com.smartvan.controller.UsuarioController;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UsuarioControllerTest {
    @Test
    void deveSalvarUsuario(){
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);//inicializa um repository falso - nao acessa o postgree
        UsuarioController usuarioController = new UsuarioController(usuarioRepository);

        //antes de salvar
        Usuario usuario = new Usuario();
        usuario.setNome("Gustavo");
        usuario.setEmail("gustavo@email.com");
        usuario.setTipo(TipoUsuario.RESPONSAVEL);

        //simulação de depois de salvar no bd
        Usuario usuarioSalvo = new Usuario();
        usuarioSalvo.setId(1L);
        usuarioSalvo.setNome("Gustavo");
        usuarioSalvo.setEmail("gustavo@email.com");
        usuarioSalvo.setTipo(TipoUsuario.RESPONSAVEL);


        when(usuarioRepository.save(usuario)).thenReturn(usuarioSalvo);//regra para quando chamar o salvarUsuario

        Usuario resultado = usuarioController.salvarUsuario(usuario);

        assertEquals(1L, resultado.getId());
        assertEquals("Gustavo", resultado.getNome());
        assertEquals("gustavo@email.com", resultado.getEmail());
        assertEquals(TipoUsuario.RESPONSAVEL, resultado.getTipo());

        verify(usuarioRepository).save(usuario);
    }
}
