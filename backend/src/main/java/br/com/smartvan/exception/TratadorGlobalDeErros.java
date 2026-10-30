package br.com.smartvan.exception;

import br.com.smartvan.dto.ErroResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/** Converte as exceções no JSON de erro do contrato (seção "Erros" do docs/CONTRATO_API.md). */
@Slf4j
@RestControllerAdvice
public class TratadorGlobalDeErros {

    /*
     * O Spring Security ainda não está no classpath principal (entra na etapa 2). Por isso o acesso negado
     * (AccessDeniedException e a subclasse AuthorizationDeniedException, lançada pelo @PreAuthorize) é
     * reconhecido pelo nome da classe. Na etapa 2 dá para trocar por um @ExceptionHandler tipado.
     */
    private static final String ACESSO_NEGADO = "org.springframework.security.access.AccessDeniedException";

    private final Clock clock;

    public TratadorGlobalDeErros(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponseDTO> regraNegocio(RegraNegocioException e) {
        return erro(e.getStatus(), e.getCodigo(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors()
                .forEach(g -> campos.putIfAbsent(g.getObjectName(), g.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ErroResponseDTO(400, "VALIDACAO", "Dados inválidos.", campos, clock.instant()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponseDTO> corpoInvalido(Exception e) {
        return erro(HttpStatus.BAD_REQUEST, "VALIDACAO", "Requisição mal formada.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> outras(Exception e) {
        if (ehAcessoNegado(e)) {
            return erro(HttpStatus.FORBIDDEN, "SEM_PERMISSAO", "Você não tem permissão para esta ação.");
        }
        // Exceções do próprio Spring MVC que já têm status (rota inexistente, método não suportado,
        // parâmetro obrigatório ausente...): mantém o status em vez de virar 500.
        if (e instanceof ErrorResponse respostaSpring) {
            HttpStatusCode status = respostaSpring.getStatusCode();
            if (status.value() == HttpStatus.NOT_FOUND.value()) {
                return erro(status, "NAO_ENCONTRADO", "Recurso não encontrado.");
            }
            if (status.is4xxClientError()) {
                return erro(status, "VALIDACAO", "Requisição inválida.");
            }
        }
        log.error("Erro inesperado", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Erro inesperado. Tente novamente.");
    }

    private ResponseEntity<ErroResponseDTO> erro(HttpStatusCode status, String codigo, String mensagem) {
        return ResponseEntity.status(status)
                .body(ErroResponseDTO.de(status.value(), codigo, mensagem, clock.instant()));
    }

    private static boolean ehAcessoNegado(Throwable e) {
        for (Class<?> c = e.getClass(); c != null; c = c.getSuperclass()) {
            if (ACESSO_NEGADO.equals(c.getName())) {
                return true;
            }
        }
        return false;
    }
}
