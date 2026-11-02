package br.com.smartvan;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.StatusMensalidade;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Mensalidade;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.MensalidadeRepository;
import br.com.smartvan.service.MensalidadeJob;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Falhas simuladas na notificação, o último passo de cada operação: se a transação não fosse desfeita, a
 * mensalidade ou a mudança de status já feitas ficariam gravadas.
 */
class MensalidadeFalhasIntegracaoTest extends TesteIntegracaoBase {

    private static final LocalDate NOVEMBRO = LocalDate.of(2026, 11, 1);
    private static final LocalDate OUTUBRO = LocalDate.of(2026, 10, 1);

    @MockitoSpyBean NotificacaoService notificacoes;
    @Autowired MensalidadeRepository mensalidades;
    @Autowired ContratoRepository contratos;
    @Autowired MensalidadeJob job;

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

    private Contrato contrato(Aluno aluno, StatusContrato status) {
        return fabrica.contrato(resp, aluno, atendimento, status);
    }

    @Test
    void falhaAoGerarDesfazOAceiteInteiroEPermiteAceitarDeNovo() throws Exception {
        Contrato c = contrato(fabrica.aluno(resp, escola), StatusContrato.PENDENTE);
        doThrow(new IllegalStateException("falha simulada")).when(spy())
                .notificar(any(), eq(TipoNotificacao.MENSALIDADE_GERADA), any(), any(), any());

        fazPostSemCorpo("/contratos/" + c.getId() + "/aceitar", mot)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"));

        assertThat(contratos.findById(c.getId()).orElseThrow().getStatus()).isEqualTo(StatusContrato.PENDENTE);
        assertThat(mensalidades.count()).isZero();
        assertThat(notificacoesDe(resp)).isEmpty();

        Mockito.reset(spy());
        fazPostSemCorpo("/contratos/" + c.getId() + "/aceitar", mot).andExpect(status().isOk());

        assertThat(contratos.findById(c.getId()).orElseThrow().getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(mensalidades.count()).isEqualTo(1);
        assertThat(notificacoesDe(resp, TipoNotificacao.PROPOSTA_ACEITA)).hasSize(1);
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_GERADA)).hasSize(1);
    }

    @Test
    void falhaNaGeracaoDeUmContratoNaoParaOsOutrosEEleTentaDeNovoDepois() {
        Aluno comFalha = fabrica.aluno(resp, escola);
        Contrato primeiro = contrato(comFalha, StatusContrato.ATIVO);
        Contrato segundo = contrato(fabrica.aluno(resp, escola), StatusContrato.ATIVO);
        doThrow(new IllegalStateException("falha simulada")).when(spy())
                .notificar(any(), eq(TipoNotificacao.MENSALIDADE_GERADA), any(), contains(comFalha.getNome()), any());

        assertThat(job.gerarCompetenciaAtual()).isEqualTo(1);

        assertThat(mensalidades.existsByContratoIdAndCompetencia(primeiro.getId(), NOVEMBRO)).isFalse();
        assertThat(mensalidades.existsByContratoIdAndCompetencia(segundo.getId(), NOVEMBRO)).isTrue();

        Mockito.reset(spy());
        assertThat(job.gerarCompetenciaAtual()).isEqualTo(1);
        assertThat(mensalidades.existsByContratoIdAndCompetencia(primeiro.getId(), NOVEMBRO)).isTrue();
    }

    @Test
    void falhaAoMarcarUmaAtrasadaNaoParaAsOutrasNemMudaOStatusDela() {
        Mensalidade comFalha = fabrica.mensalidade(contrato(fabrica.aluno(resp, escola), StatusContrato.ATIVO),
                OUTUBRO, StatusMensalidade.PENDENTE);
        Mensalidade outra = fabrica.mensalidade(contrato(fabrica.aluno(resp, escola), StatusContrato.ATIVO),
                OUTUBRO, StatusMensalidade.PENDENTE);
        doThrow(new IllegalStateException("falha simulada")).when(spy())
                .notificar(any(), eq(TipoNotificacao.MENSALIDADE_ATRASADA), any(), any(), eq(comFalha.getId()));

        assertThat(job.marcarAtrasadas()).isEqualTo(1);

        assertThat(mensalidades.findById(comFalha.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusMensalidade.PENDENTE);
        assertThat(mensalidades.findById(outra.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusMensalidade.ATRASADA);

        Mockito.reset(spy());
        assertThat(job.marcarAtrasadas()).isEqualTo(1);
    }

    @Test
    void falhaNaGeracaoNaoImpedeAMarcacaoDeAtrasoNaMesmaExecucao() {
        Contrato c = contrato(fabrica.aluno(resp, escola), StatusContrato.ATIVO);
        Mensalidade outubro = fabrica.mensalidade(c, OUTUBRO, StatusMensalidade.PENDENTE);
        doThrow(new IllegalStateException("falha simulada")).when(spy())
                .notificar(any(), eq(TipoNotificacao.MENSALIDADE_GERADA), any(), any(), any());

        job.executar();

        assertThat(mensalidades.existsByContratoIdAndCompetencia(c.getId(), NOVEMBRO)).isFalse();
        assertThat(mensalidades.findById(outubro.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusMensalidade.ATRASADA);
    }

    @Test
    void falhaNaBaixaDesfazOPagamentoEAAuditoria() throws Exception {
        Mensalidade m = fabrica.mensalidade(contrato(fabrica.aluno(resp, escola), StatusContrato.ATIVO),
                NOVEMBRO, StatusMensalidade.PENDENTE);
        doThrow(new IllegalStateException("falha simulada")).when(spy())
                .notificar(any(), eq(TipoNotificacao.MENSALIDADE_PAGA), any(), any(), any());

        fazPostSemCorpo("/mensalidades/" + m.getId() + "/baixa", mot).andExpect(status().isInternalServerError());

        Mensalidade relida = mensalidades.findById(m.getId()).orElseThrow();
        assertThat(relida.getStatus()).isEqualTo(StatusMensalidade.PENDENTE);
        assertThat(relida.getDataPagamento()).isNull();
        assertThat(auditorias("Mensalidade", m.getId())).isEmpty();
    }
}
