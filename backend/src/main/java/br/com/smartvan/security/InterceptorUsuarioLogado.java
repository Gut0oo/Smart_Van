package br.com.smartvan.security;

import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.Set;

/**
 * Identifica quem chama a API pelo header {@value #HEADER} e confere o {@link Papel} da rota.
 * Toda rota é protegida, exceto as de {@link #ROTAS_PUBLICAS}.
 *
 * ATENÇÃO: o header não prova a identidade de quem chama (qualquer um pode enviar o id de outra pessoa).
 * Uso acadêmico; não serve para produção. Ver "Convenções" no docs/CONTRATO_API.md.
 */
public class InterceptorUsuarioLogado implements HandlerInterceptor {

    public static final String HEADER = "X-Usuario-Id";

    private static final Set<String> ROTAS_PUBLICAS = Set.of(
            "POST /usuarios",
            "POST /auth/login",
            "GET /escolas",
            "GET /actuator/health");

    private final UsuarioRepository usuarios;

    public InterceptorUsuarioLogado(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //o erro original já foi tratado na requisição que o causou
        if (request.getDispatcherType() == DispatcherType.ERROR || ehPublica(request)) {
            return true;
        }
        Usuario usuario = carregar(request.getHeader(HEADER));
        if (usuario.getStatus() == StatusConta.BLOQUEADA) {
            throw new RegraNegocioException(HttpStatus.FORBIDDEN, "CONTA_BLOQUEADA", "Sua conta está bloqueada.");
        }
        conferirPapel(handler, usuario);
        //por último: quando o preHandle lança exceção, o afterCompletion deste interceptor não roda
        UsuarioLogado.definir(new UsuarioLogado(usuario.getId(), usuario.getTipo()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        UsuarioLogado.limpar();
    }

    private static boolean ehPublica(HttpServletRequest request) {
        return ROTAS_PUBLICAS.contains(request.getMethod() + " " + request.getRequestURI());
    }

    private Usuario carregar(String header) {
        return paraId(header).flatMap(usuarios::findById)
                .orElseThrow(() -> new RegraNegocioException(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO",
                        "Faça login para continuar."));
    }

    private static Optional<Long> paraId(String header) {
        if (header == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(header));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static void conferirPapel(Object handler, Usuario usuario) {
        if (handler instanceof HandlerMethod metodo) {
            Papel papel = metodo.getMethodAnnotation(Papel.class);
            if (papel == null) {
                papel = AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(), Papel.class);
            }
            if (papel != null && papel.value() != usuario.getTipo()) {
                throw RegraNegocioException.semPermissao();
            }
        }
    }
}
