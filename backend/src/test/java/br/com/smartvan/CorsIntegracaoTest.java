package br.com.smartvan;

import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O app web (Expo no navegador) consegue chamar a API; outras origens não. */
class CorsIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void preVooDoAppWebPassaSemOHeaderDoUsuario() throws Exception {
        mvc.perform(options("/contratos")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "x-usuario-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void respostaLevaACabecalhoParaOAppWeb() throws Exception {
        mvc.perform(get("/escolas").header("Origin", "http://localhost:8081"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void origemDesconhecidaERecusada() throws Exception {
        mvc.perform(options("/contratos")
                        .header("Origin", "http://site-estranho.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void preVooNaoLiberaARotaDeVerdade() throws Exception {
        mvc.perform(get("/contratos").header("Origin", "http://localhost:8081"))
                .andExpect(status().isUnauthorized());
    }
}
