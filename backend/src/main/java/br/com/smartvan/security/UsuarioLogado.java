package br.com.smartvan.security;

import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.exception.RegraNegocioException;
import org.springframework.http.HttpStatus;

/**
 * Quem está chamando a API. Uso nos controllers/services: {@code UsuarioLogado eu = UsuarioLogado.atual();}
 *
 * PROVISÓRIO (etapa 1): ainda não existe login, então {@link #atual()} sempre responde 401 NAO_AUTENTICADO.
 * A etapa 2 troca o corpo de {@link #atual()} para ler o usuário do contexto do Spring Security
 * (preenchido pelo filtro do token de sessão), mantendo esta assinatura.
 */
public record UsuarioLogado(Long id, TipoUsuario tipo) {

    public static UsuarioLogado atual() {
        throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", "Faça login para continuar.");
    }

    public boolean ehMotorista() {
        return tipo == TipoUsuario.MOTORISTA;
    }

    public boolean ehResponsavel() {
        return tipo == TipoUsuario.RESPONSAVEL;
    }
}
