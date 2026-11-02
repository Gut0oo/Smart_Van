package br.com.smartvan;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Mensalidade;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.repository.MensalidadeRepository;
import br.com.smartvan.service.MensalidadeJob;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** A regra do "hoje + 5" compara com o dia configurado, não com um 10 fixo. */
@TestPropertySource(properties = "smartvan.mensalidade.dia-vencimento=20")
class MensalidadeDiaConfiguradoIntegracaoTest extends TesteIntegracaoBase {

    @Autowired MensalidadeJob job;
    @Autowired MensalidadeRepository mensalidades;

    Contrato contrato;

    @BeforeEach
    void cenario() {
        Responsavel r = fabrica.responsavel();
        Motorista m = fabrica.motorista();
        var escola = fabrica.escola();
        contrato = fabrica.contrato(r, fabrica.aluno(r, escola),
                fabrica.atendimento(m, escola, LocalTime.of(7, 0), LocalTime.of(6, 0)), StatusContrato.ATIVO);
    }

    private LocalDate vencimentoGeradoEm(LocalDate hoje) {
        relogio.definirData(hoje);
        job.gerarCompetenciaAtual();
        Mensalidade m = mensalidades.doContrato(contrato.getId()).getFirst();
        return m.getVencimento();
    }

    @Test
    void depoisDoDia10EAntesDoDiaConfiguradoVenceNoDiaConfigurado() {
        assertThat(vencimentoGeradoEm(LocalDate.of(2026, 11, 15))).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void noDiaConfiguradoAindaVenceNele() {
        assertThat(vencimentoGeradoEm(LocalDate.of(2026, 11, 20))).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    void depoisDoDiaConfiguradoVenceEmHojeMaisCinco() {
        assertThat(vencimentoGeradoEm(LocalDate.of(2026, 11, 21))).isEqualTo(LocalDate.of(2026, 11, 26));
    }
}
