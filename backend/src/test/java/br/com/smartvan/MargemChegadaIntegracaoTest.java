package br.com.smartvan;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//a margem padrão (15) é coberta no CadastroIntegracaoTest; aqui, que ela vem da configuração
@TestPropertySource(properties = "smartvan.rota.margem-chegada-minutos=30")
class MargemChegadaIntegracaoTest extends TesteIntegracaoBase {

    @Test
    void tetoDeChegadaUsaAMargemConfigurada() throws Exception {
        Motorista m = fabrica.motorista();
        fabrica.atenderEscolas(m, fabrica.escola()); //aula às 07:00

        fazGet("/motorista/escolas", m).andExpect(jsonPath("$[0].tetoChegada").value("06:30"));
    }

    @Test
    void rotaUsaOTetoDaMargemConfigurada() throws Exception {
        Motorista m = fabrica.motorista();
        fabrica.enderecoPartida(m);
        fabrica.veiculo(m, 10);
        Escola escola = fabrica.escola();
        //aula 07:00, teto 06:30; com um aluno, o percurso do mapa falso leva 10 minutos
        Atendimento a = fabrica.atendimento(m, escola, LocalTime.of(7, 0), LocalTime.of(6, 25));
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, escola), a, StatusContrato.ATIVO);

        fazPost("/motorista/rotas/gerar",
                Map.of("data", relogio.hoje().plusDays(1).toString(), "tipo", "IDA", "atendimentoId", a.getId()), m)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horarioFim").value("06:35"))
                .andExpect(jsonPath("$.foraDoTeto").value(true))
                .andExpect(jsonPath("$.minutosAdiantar").value(5));
    }
}
