package br.com.smartvan.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exceção padrão de regra de negócio. O {@link TratadorGlobalDeErros} transforma em ErroResponseDTO.
 * Use os códigos definidos em docs/CONTRATO_API.md.
 */
@Getter
public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public RegraNegocioException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    /** 404 NAO_ENCONTRADO. Ex.: {@code naoEncontrado("Notificação")}. */
    public static RegraNegocioException naoEncontrado(String recurso) {
        return new RegraNegocioException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", recurso + " não encontrado(a).");
    }

    /** 403 SEM_PERMISSAO (papel errado ou recurso de outra pessoa). */
    public static RegraNegocioException semPermissao() {
        return new RegraNegocioException(HttpStatus.FORBIDDEN, "SEM_PERMISSAO", "Você não tem permissão para esta ação.");
    }

    /** 409 com o código do endpoint (conflito de estado / duplicidade). */
    public static RegraNegocioException conflito(String codigo, String mensagem) {
        return new RegraNegocioException(HttpStatus.CONFLICT, codigo, mensagem);
    }

    /** 422 com o código do endpoint (regra de negócio não atendida). */
    public static RegraNegocioException regra(String codigo, String mensagem) {
        return new RegraNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, codigo, mensagem);
    }
}
