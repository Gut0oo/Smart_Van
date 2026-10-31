package br.com.smartvan.security;

import br.com.smartvan.enums.TipoUsuario;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Papel exigido pela rota, conferido no {@link InterceptorUsuarioLogado}. Papel errado = 403 SEM_PERMISSAO.
 * Vale no método ou no controller inteiro; se houver nos dois, o do método prevalece.
 * Ex.: {@code @Papel(TipoUsuario.MOTORISTA)}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Papel {

    TipoUsuario value();
}
