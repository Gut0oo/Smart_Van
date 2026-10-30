package br.com.smartvan;

import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TratadorGlobalDeErros: o JSON de erro da seção "Erros" do contrato.
 *
 * As rotas de teste são protegidas como qualquer outra, por isso as requisições levam o header X-Usuario-Id.
 */
@Import(ErrosIntegracaoTest.ControllerDeTeste.class)
class ErrosIntegracaoTest extends TesteIntegracaoBase {

    private Responsavel usuario;

    @BeforeEach
    void criarUsuario() {
        usuario = fabrica.responsavel();
    }

    @Test
    void regraNegocioViraJsonComStatusCodigoMensagemETimestamp() throws Exception {
        fazGet("/teste-erros/regra", usuario)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.codigo").value("SEM_VAGAS"))
                .andExpect(jsonPath("$.mensagem").value("O motorista não tem vagas disponíveis."))
                .andExpect(jsonPath("$.timestamp").value("2026-11-03T12:00:00Z"))
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    void regraNaoAtendidaUsa422() throws Exception {
        fazGet("/teste-erros/regra-422", usuario)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.codigo").value("CODIGO_DA_REGRA"));
    }

    @Test
    void beanValidationDa400ValidacaoComCampos() throws Exception {
        mvc.perform(autenticar(post("/teste-erros/validacao"), usuario)
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
        mvc.perform(autenticar(post("/teste-erros/validacao"), usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nao é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void semPermissaoDa403() throws Exception {
        fazGet("/teste-erros/sem-permissao", usuario)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void rotaInexistenteDa404NaoEncontrado() throws Exception {
        fazGet("/rota-que-nao-existe", usuario)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void erroInesperadoDa500SemExporDetalhes() throws Exception {
        fazGet("/teste-erros/inesperado", usuario)
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

        @GetMapping("/sem-permissao")
        public void semPermissao() {
            throw RegraNegocioException.semPermissao();
        }

        @GetMapping("/inesperado")
        public void inesperado() {
            throw new IllegalStateException("detalhe interno que não pode vazar");
        }
    }
}
