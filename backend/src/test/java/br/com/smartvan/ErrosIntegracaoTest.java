package br.com.smartvan;

import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TratadorGlobalDeErros: o JSON de erro da seção "Erros" do contrato.
 *
 * O spring-boot-starter-security-test do pom já põe o Spring Security no classpath de TESTE (com a configuração
 * padrão: tudo exige login e POST exige CSRF). Por isso o teste usa @WithMockUser e csrf(); os dois continuam
 * válidos depois que a etapa 2 ligar a segurança de verdade.
 */
@WithMockUser
@Import(ErrosIntegracaoTest.ControllerDeTeste.class)
class ErrosIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void regraNegocioViraJsonComStatusCodigoMensagemETimestamp() throws Exception {
        mvc.perform(get("/teste-erros/regra"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("SEM_VAGAS"))
                .andExpect(jsonPath("$.mensagem").value("O motorista não tem vagas disponíveis."))
                .andExpect(jsonPath("$.timestamp").value("2026-11-03T12:00:00Z"))
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    void regraNaoAtendidaUsa422() throws Exception {
        mvc.perform(get("/teste-erros/regra-422"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.codigo").value("CODIGO_DA_REGRA"));
    }

    @Test
    void beanValidationDa400ValidacaoComCampos() throws Exception {
        mvc.perform(post("/teste-erros/validacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void corpoMalFormadoDa400Validacao() throws Exception {
        mvc.perform(post("/teste-erros/validacao").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nao é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void acessoNegadoDa403SemPermissao() throws Exception {
        mvc.perform(get("/teste-erros/acesso-negado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void rotaInexistenteDa404NaoEncontrado() throws Exception {
        mvc.perform(get("/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void erroInesperadoDa500SemExporDetalhes() throws Exception {
        mvc.perform(get("/teste-erros/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value("Erro inesperado. Tente novamente."));
    }

    // ---------------------------------------------------------------- controller só deste teste

    public record CorpoDeTeste(@NotBlank String nome, @Email String email) {
    }

    @RestController
    @RequestMapping("/teste-erros")
    public static class ControllerDeTeste {

        @GetMapping("/regra")
        public void regra() {
            throw RegraNegocioException.conflito("SEM_VAGAS", "O motorista não tem vagas disponíveis.");
        }

        @GetMapping("/regra-422")
        public void regra422() {
            throw RegraNegocioException.regra("CODIGO_DA_REGRA", "Regra não atendida.");
        }

        @PostMapping("/validacao")
        public void validacao(@Valid @RequestBody CorpoDeTeste corpo) {
        }

        @GetMapping("/acesso-negado")
        public void acessoNegado() {
            throw new AccessDeniedException("negado");
        }

        @GetMapping("/inesperado")
        public void inesperado() {
            throw new IllegalStateException("detalhe interno que não pode vazar");
        }
    }
}
