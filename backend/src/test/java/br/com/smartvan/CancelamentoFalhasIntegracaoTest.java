package br.com.smartvan;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.service.CancelamentoJob;
import br.com.smartvan.service.NotificacaoService;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Falhas simuladas na notificação, o último passo de cada operação: a mudança já feita precisa ser desfeita. */
class CancelamentoFalhasIntegracaoTest extends TesteIntegracaoBase {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 3);
    private static final LocalDate ENCERRAMENTO = LocalDate.of(2026, 12, 3);

    @MockitoSpyBean NotificacaoService notificacoes;
    @Autowired ContratoRepository contratos;
    @Autowired CancelamentoJob job;

    Responsavel resp;
    Motorista mot;
    Escola escola;
    Atendimento atendimento;

    @BeforeEach
    void cenario() {
        resp = fabrica.responsavel();
        mot = fabrica.motorista();
        fabrica.veiculo(mot, 10);
        escola = fabrica.escola();
        atendimento = fabrica.atendimento(mot, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
    }

    //o service é proxy de transação; o stub vai no spy que está dentro dele
    private NotificacaoService spy() {
        return AopTestUtils.getUltimateTargetObject(notificacoes);
    }

    private Contrato contrato(Aluno aluno) {
        return fabrica.contrato(resp, aluno, atendimento, StatusContrato.ATIVO);
    }

    private Contrato comAviso() {
        return fabrica.comCancelamento(contrato(fabrica.aluno(resp, escola)), TipoUsuario.RESPONSAVEL, HOJE);
    }

    private StatusContrato statusDe(Contrato c) {
        return contratos.findById(c.getId()).orElseThrow().getStatus();
    }

    @Test
    void falhaAoNotificarDesfazASolicitacaoEPermiteSolicitarDeNovo() throws Exception {
        Contrato c = contrato(fabrica.aluno(resp, escola));
        doThrow(new IllegalStateException("falha simulada")).when(spy())
                .notificar(any(), eq(TipoNotificacao.CANCELAMENTO_SOLICITADO), any(), any(), any());
        String url = "/contratos/" + c.getId() + "/cancelamento";

        fazPost(url, Map.of("motivo", "Mudança de cidade"), resp)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        Contrato relido = contratos.findById(c.getId()).orElseThrow();
        assertThat(relido.getDataSolicitacaoCancelamento()).isNull();
        assertThat(relido.getDataEncerramentoEfetivo()).isNull();
        assertThat(relido.getSolicitanteCancelamento()).isNull();
        assertThat(relido.getMotivoCancelamento()).isNull();

        Mockito.reset(spy());
        fazPost(url, Map.of("motivo", "Mudança de cidade"), resp).andExpect(status().isOk());
    }

    @Test
    void falhaEmUmContratoNaoParaOsOutrosEEleTentaDeNovoDepois() {
        Contrato comFalha = comAviso();
        Contrato outro = comAviso();
        doThrow(new IllegalStateException("falha simulada")).when(spy())
                .notificar(any(), eq(TipoNotificacao.CONTRATO_ENCERRADO), any(), any(), eq(comFalha.getId()));
        relogio.definirData(ENCERRAMENTO);

        assertThat(job.encerrarVencidos()).isEqualTo(1);

        assertThat(statusDe(comFalha)).isEqualTo(StatusContrato.ATIVO);
        assertThat(statusDe(outro)).isEqualTo(StatusContrato.ENCERRADO);

        Mockito.reset(spy());
        assertThat(job.encerrarVencidos()).isEqualTo(1);
        assertThat(statusDe(comFalha)).isEqualTo(StatusContrato.ENCERRADO);
    }

    @Test
    void falhaAoNotificarASegundaParteDesfazOEncerramentoEAPrimeiraNotificacao() {
        Contrato c = comAviso();
        doThrow(new IllegalStateException("falha simulada")).when(spy())
                .notificar(eq(mot.getId()), eq(TipoNotificacao.CONTRATO_ENCERRADO), any(), any(), any());
        relogio.definirData(ENCERRAMENTO);

        assertThat(job.encerrarVencidos()).isZero();

        assertThat(statusDe(c)).isEqualTo(StatusContrato.ATIVO);
        assertThat(notificacoesDe(resp)).isEmpty();
    }
}
