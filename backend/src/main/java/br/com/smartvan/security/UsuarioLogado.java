package br.com.smartvan.security;

import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.exception.RegraNegocioException;
import org.springframework.http.HttpStatus;

/**
 * Quem está chamando a API (preenchido pelo {@link InterceptorUsuarioLogado} e limpo ao fim da requisição).
 * Uso nos controllers/services: {@code UsuarioLogado eu = UsuarioLogado.atual();}
 */
public record UsuarioLogado(Long id, TipoUsuario tipo) {

    private static final ThreadLocal<UsuarioLogado> ATUAL = new ThreadLocal<>();

    public static UsuarioLogado atual() {
        UsuarioLogado usuario = ATUAL.get();
        if (usuario == null) {
            throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Faça login para continuar.");
        }
        return usuario;
    }

    static void definir(UsuarioLogado usuario) {
        ATUAL.set(usuario);
    }

    static void limpar() {
        ATUAL.remove();
    }

    public boolean ehMotorista() {
        return tipo == TipoUsuario.MOTORISTA;
    }

    public boolean ehResponsavel() {
        return tipo == TipoUsuario.RESPONSAVEL;
    }
}
