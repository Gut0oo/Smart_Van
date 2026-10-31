package br.com.smartvan;

import br.com.smartvan.model.Motorista;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

//a margem padrão (15) é coberta no CadastroIntegracaoTest; aqui, que ela vem da configuração
@TestPropertySource(properties = "smartvan.rota.margem-chegada-minutos=30")
class MargemChegadaIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void tetoDeChegadaUsaAMargemConfigurada() throws Exception {
        Motorista m = fabrica.motorista();
        fabrica.atenderEscolas(m, fabrica.escola()); //aula às 07:00

        fazGet("/motorista/escolas", m).andExpect(jsonPath("$[0].tetoChegada").value("06:30"));
    }
}
