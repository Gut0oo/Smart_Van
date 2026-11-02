package br.com.smartvan;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.StatusMensalidade;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.event.ContratoAtivadoEvent;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Mensalidade;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.MensalidadeRepository;
import br.com.smartvan.service.MensalidadeConsultaService;
import br.com.smartvan.service.MensalidadeJob;
import br.com.smartvan.service.MensalidadeService;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O relógio começa em 2026-11-03; o dia de vencimento dos testes é 10. */
class MensalidadeIntegracaoTest extends TesteIntegracaoBase {

    private static final LocalDate NOVEMBRO = LocalDate.of(2026, 11, 1);
    private static final LocalDate OUTUBRO = LocalDate.of(2026, 10, 1);
    private static final LocalDate SETEMBRO = LocalDate.of(2026, 9, 1);

    @Autowired ApplicationEventPublisher publicador;
    @Autowired MensalidadeJob job;
    @Autowired MensalidadeService service;
    @Autowired MensalidadeRepository mensalidades;
    @Autowired ContratoRepository contratos;
    @Autowired MensalidadeConsultaService consulta;
    @Autowired TransactionTemplate tx;

    Responsavel resp;
    Motorista mot;
    Escola escola;
    Atendimento atendimento;
    Aluno aluno;

    @BeforeEach
    void cenario() {
        resp = fabrica.responsavel();
        mot = fabrica.motorista();
        fabrica.veiculo(mot, 10);
        escola = fabrica.escola();
        atendimento = fabrica.atendimento(mot, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        aluno = fabrica.aluno(resp, escola);
    }

    private Contrato contrato(StatusContrato status) {
        return fabrica.contrato(resp, fabrica.aluno(resp, escola), atendimento, status);
    }

    private Contrato ativo() {
        return fabrica.contrato(resp, aluno, atendimento, StatusContrato.ATIVO);
    }

    private Mensalidade unica() {
        List<Mensalidade> todas = mensalidades.findAll();
        assertThat(todas).hasSize(1);
        return todas.getFirst();
    }

    private StatusMensalidade statusDe(Mensalidade m) {
        return mensalidades.findById(m.getId()).orElseThrow().getStatus();
    }

    private String baixa(Mensalidade m) {
        return "/mensalidades/" + m.getId() + "/baixa";
    }

    // ---------------------------------------------------------------- geração ao ativar o contrato

    @Test
    void aceiteDoContratoGeraAMensalidadeDaCompetenciaAtualENotifica() throws Exception {
        Contrato c = fabrica.contrato(resp, aluno, atendimento, StatusContrato.PENDENTE);

        fazPostSemCorpo("/contratos/" + c.getId() + "/aceitar", mot).andExpect(status().isOk());

        Mensalidade m = unica();
        assertThat(m.getCompetencia()).isEqualTo(NOVEMBRO);
        assertThat(m.getVencimento()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(m.getStatus()).isEqualTo(StatusMensalidade.PENDENTE);
        assertThat(m.getValor()).isEqualByComparingTo("350.00");
        assertThat(m.getDataPagamento()).isNull();
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_GERADA)).singleElement()
                .satisfies(n -> assertThat(n.getReferenciaId()).isEqualTo(m.getId()));
    }

    @Test
    void eventoRepetidoNaoDuplicaAMensalidadeNemANotificacao() {
        Contrato c = ativo();

        publicador.publishEvent(new ContratoAtivadoEvent(c.getId()));
        publicador.publishEvent(new ContratoAtivadoEvent(c.getId()));

        unica();
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_GERADA)).hasSize(1);
    }

    @Test
    void eventoDeContratoQueNaoEstaAtivoNaoGera() {
        Contrato c = contrato(StatusContrato.ENCERRADO);

        publicador.publishEvent(new ContratoAtivadoEvent(c.getId()));

        assertThat(mensalidades.count()).isZero();
    }

    @Test
    void primeiraMensalidadeVenceNoDiaDaAssinatura() {
        publicador.publishEvent(new ContratoAtivadoEvent(ativo().getId()));

        assertThat(unica().getCompetencia()).isEqualTo(NOVEMBRO);
        assertThat(unica().getVencimento()).isEqualTo(LocalDate.of(2026, 11, 3));
    }

    // ---------------------------------------------------------------- agendador: geração

    @Test
    void agendadorGeraSoParaContratoAtivoENotifica() {
        Contrato ativo = ativo();
        contrato(StatusContrato.PENDENTE);
        contrato(StatusContrato.RECUSADO);
        contrato(StatusContrato.ENCERRADO);

        assertThat(job.gerarCompetenciaAtual()).isEqualTo(1);

        assertThat(mensalidades.existsByContratoIdAndCompetencia(ativo.getId(), NOVEMBRO)).isTrue();
        unica();
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_GERADA)).hasSize(1);
    }

    @Test
    void agendadorEhIdempotente() {
        ativo();

        assertThat(job.gerarCompetenciaAtual()).isEqualTo(1);
        assertThat(job.gerarCompetenciaAtual()).isZero();

        unica();
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_GERADA)).hasSize(1);
    }

    @Test
    void uniqueNoBancoBarraContratoECompetenciaRepetidos() {
        Contrato c = ativo();
        fabrica.mensalidade(c, NOVEMBRO, StatusMensalidade.PENDENTE);

        assertThatThrownBy(() -> fabrica.mensalidade(c, NOVEMBRO, StatusMensalidade.PENDENTE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mensalidadeDoMesSeguinteNasceNoMesmoDiaDaAssinatura() {
        Contrato c = ativo();
        job.gerarCompetenciaAtual();

        relogio.definirData(LocalDate.of(2026, 12, 2));
        assertThat(job.gerarCompetenciaAtual()).isZero();

        relogio.definirData(LocalDate.of(2026, 12, 3));
        assertThat(job.gerarCompetenciaAtual()).isEqualTo(1);

        assertThat(mensalidades.doContrato(c.getId())).extracting(Mensalidade::getVencimento)
                .containsExactly(LocalDate.of(2026, 12, 3), LocalDate.of(2026, 11, 3));
    }

    @Test
    void emMesMaisCurtoVenceNoUltimoDia() {
        Contrato c = ativo();
        c.setDataInicio(LocalDate.of(2026, 10, 31));
        contratos.save(c);

        relogio.definirData(LocalDate.of(2026, 11, 29));
        assertThat(job.gerarCompetenciaAtual()).isZero();

        relogio.definirData(LocalDate.of(2026, 11, 30));
        assertThat(job.gerarCompetenciaAtual()).isEqualTo(1);
        assertThat(unica().getVencimento()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void contratoQueEncerraAteOVencimentoNaoGeraOMesSemServico() {
        Contrato c = fabrica.comCancelamento(ativo(), TipoUsuario.RESPONSAVEL, LocalDate.of(2026, 11, 3));
        assertThat(c.getDataEncerramentoEfetivo()).isEqualTo(LocalDate.of(2026, 12, 3));

        relogio.definirData(LocalDate.of(2026, 12, 3));
        assertThat(job.gerarCompetenciaAtual()).isZero();

        assertThat(mensalidades.doContrato(c.getId())).isEmpty();
    }

    @Test
    void contratoComCancelamentoSolicitadoContinuaGerando() {
        Contrato c = ativo();
        c.setDataSolicitacaoCancelamento(relogio.hoje());
        c.setDataEncerramentoEfetivo(relogio.hoje().plusDays(30));
        c.setSolicitanteCancelamento(TipoUsuario.RESPONSAVEL);
        c.setMotivoCancelamento("Mudança de cidade");
        contratos.save(c);

        assertThat(job.gerarCompetenciaAtual()).isEqualTo(1);
        unica();
    }

    // ---------------------------------------------------------------- agendador: atraso

    @Test
    void marcaAtrasadaSoDepoisDoVencimentoENotificaUmaVez() {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);

        relogio.definirData(LocalDate.of(2026, 11, 10));
        assertThat(job.marcarAtrasadas()).isZero();
        assertThat(statusDe(m)).isEqualTo(StatusMensalidade.PENDENTE);

        relogio.definirData(LocalDate.of(2026, 11, 11));
        assertThat(job.marcarAtrasadas()).isEqualTo(1);
        assertThat(job.marcarAtrasadas()).isZero();

        assertThat(statusDe(m)).isEqualTo(StatusMensalidade.ATRASADA);
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_ATRASADA)).singleElement()
                .satisfies(n -> assertThat(n.getReferenciaId()).isEqualTo(m.getId()));
    }

    @Test
    void naoMarcaComoAtrasadaOQueJaFoiPago() {
        Mensalidade paga = fabrica.mensalidade(ativo(), OUTUBRO, StatusMensalidade.PAGA);

        assertThat(job.marcarAtrasadas()).isZero();

        assertThat(statusDe(paga)).isEqualTo(StatusMensalidade.PAGA);
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_ATRASADA)).isEmpty();
    }

    @Test
    void execucaoDiariaGeraACompetenciaEMarcaAsVencidas() {
        Contrato c = ativo();
        Mensalidade outubro = fabrica.mensalidade(c, OUTUBRO, StatusMensalidade.PENDENTE);

        job.executar();

        assertThat(statusDe(outubro)).isEqualTo(StatusMensalidade.ATRASADA);
        assertThat(mensalidades.existsByContratoIdAndCompetencia(c.getId(), NOVEMBRO)).isTrue();
        assertThat(mensalidades.count()).isEqualTo(2);
    }

    @Test
    void agendadorParadoGeraComOVencimentoDoContratoEMarcaAtraso() {
        ativo();
        relogio.definirData(LocalDate.of(2026, 11, 20));

        job.executar();

        Mensalidade m = unica();
        assertThat(m.getVencimento()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(m.getStatus()).isEqualTo(StatusMensalidade.ATRASADA);
    }

    // ---------------------------------------------------------------- baixa

    @Test
    void baixaMarcaPagaGravaAuditoriaENotifica() throws Exception {
        Contrato c = ativo();
        Mensalidade m = fabrica.mensalidade(c, NOVEMBRO, StatusMensalidade.PENDENTE);

        fazPost(baixa(m), Map.of("dataPagamento", "2026-11-02"), mot)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(m.getId()))
                .andExpect(jsonPath("$.contratoId").value(c.getId()))
                .andExpect(jsonPath("$.competencia").value("2026-11-01"))
                .andExpect(jsonPath("$.valor").value(350.00))
                .andExpect(jsonPath("$.vencimento").value("2026-11-10"))
                .andExpect(jsonPath("$.status").value("PAGA"))
                .andExpect(jsonPath("$.dataPagamento").value("2026-11-02"))
                .andExpect(jsonPath("$.aluno.id").value(aluno.getId()))
                .andExpect(jsonPath("$.aluno.nome").value(aluno.getNome()))
                .andExpect(jsonPath("$.responsavel.id").value(resp.getId()))
                .andExpect(jsonPath("$.responsavel.telefone").value(resp.getTelefone()))
                .andExpect(jsonPath("$.motorista.id").value(mot.getId()));

        assertThat(auditorias("Mensalidade", m.getId())).singleElement().satisfies(a -> {
            assertThat(a.getAcao()).isEqualTo("BAIXA_MENSALIDADE");
            assertThat(a.getUsuarioId()).isEqualTo(mot.getId());
        });
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_PAGA)).singleElement()
                .satisfies(n -> assertThat(n.getReferenciaId()).isEqualTo(m.getId()));
        assertThat(mensalidades.findById(m.getId()).orElseThrow().getDataPagamento())
                .isEqualTo(LocalDate.of(2026, 11, 2));
    }

    @Test
    void baixaSemCorpoUsaHojeEAceitaAtrasada() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), OUTUBRO, StatusMensalidade.ATRASADA);

        fazPostSemCorpo(baixa(m), mot)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGA"))
                .andExpect(jsonPath("$.dataPagamento").value("2026-11-03"));
    }

    @Test
    void baixaComDataDeHojeEhAceita() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);

        fazPost(baixa(m), Map.of("dataPagamento", "2026-11-03"), mot).andExpect(status().isOk());
    }

    @Test
    void baixaDeMensalidadeJaPagaDevolve409() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PAGA);

        fazPostSemCorpo(baixa(m), mot)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MENSALIDADE_JA_PAGA"));
        assertThat(auditorias("Mensalidade", m.getId())).isEmpty();
    }

    @Test
    void baixaComDataFuturaDevolve422ENaoMudaNada() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);

        fazPost(baixa(m), Map.of("dataPagamento", "2026-11-04"), mot)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("DATA_FUTURA"));

        assertThat(statusDe(m)).isEqualTo(StatusMensalidade.PENDENTE);
        assertThat(auditorias("Mensalidade", m.getId())).isEmpty();
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_PAGA)).isEmpty();
    }

    @Test
    void baixaDeOutroMotoristaDevolve403() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);

        fazPostSemCorpo(baixa(m), fabrica.motorista())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));

        assertThat(statusDe(m)).isEqualTo(StatusMensalidade.PENDENTE);
    }

    @Test
    void baixaPorQuemNaoEhMotoristaDevolve403() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);

        fazPostSemCorpo(baixa(m), resp)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void baixaSemIdentificacaoDevolve401() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);

        fazPost(baixa(m))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void baixaDeMensalidadeInexistenteDevolve404() throws Exception {
        fazPostSemCorpo("/mensalidades/99999/baixa", mot)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void baixaComDataMalFormadaDevolve400() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);

        fazPost(baixa(m), Map.of("dataPagamento", "ontem"), mot)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void baixasSimultaneasSoUmaPassaEGravaUmaAuditoria() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);

        List<Integer> statusHttp = simultaneos(List.of(
                () -> fazPostSemCorpo(baixa(m), mot).andReturn().getResponse().getStatus(),
                () -> fazPostSemCorpo(baixa(m), mot).andReturn().getResponse().getStatus(),
                () -> fazPostSemCorpo(baixa(m), mot).andReturn().getResponse().getStatus(),
                () -> fazPostSemCorpo(baixa(m), mot).andReturn().getResponse().getStatus()));

        assertThat(statusHttp).containsExactlyInAnyOrder(200, 409, 409, 409);
        assertThat(auditorias("Mensalidade", m.getId())).hasSize(1);
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_PAGA)).hasSize(1);
    }

    @Test
    void agendadorEsperaABaixaEmAndamentoENaoMarcaAtraso() throws Exception {
        Mensalidade m = fabrica.mensalidade(ativo(), OUTUBRO, StatusMensalidade.PENDENTE);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            //a baixa fica aberta enquanto o agendador roda: 300 ms é menos que o lock timeout do H2
            Future<Integer> marcadas = tx.execute(status -> {
                service.darBaixa(mot.getId(), m.getId(), null);
                Future<Integer> execucao = pool.submit(job::marcarAtrasadas);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
                return execucao;
            });

            assertThat(marcadas.get(10, TimeUnit.SECONDS)).isZero();
        } finally {
            pool.shutdownNow();
        }
        assertThat(statusDe(m)).isEqualTo(StatusMensalidade.PAGA);
        assertThat(notificacoesDe(resp, TipoNotificacao.MENSALIDADE_ATRASADA)).isEmpty();
    }

    private List<Integer> simultaneos(List<Callable<Integer>> tarefas) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Callable<Integer> tarefa : tarefas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return tarefa.call();
                }));
            }
            largada.countDown();
            List<Integer> resultados = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- listagens

    @Test
    void listagemDoContratoVemEmCompetenciaDecrescenteParaAsDuasPartes() throws Exception {
        Contrato c = ativo();
        fabrica.mensalidade(c, SETEMBRO, StatusMensalidade.PAGA);
        fabrica.mensalidade(c, NOVEMBRO, StatusMensalidade.PENDENTE);
        fabrica.mensalidade(c, OUTUBRO, StatusMensalidade.PAGA);

        for (Usuario parte : List.of(resp, mot)) {
            fazGet("/contratos/" + c.getId() + "/mensalidades", parte)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].competencia").value("2026-11-01"))
                    .andExpect(jsonPath("$[1].competencia").value("2026-10-01"))
                    .andExpect(jsonPath("$[2].competencia").value("2026-09-01"));
        }
    }

    @Test
    void listagemDoContratoRecusaQuemNaoEhParteEContratoInexistente() throws Exception {
        Contrato c = ativo();

        fazGet("/contratos/" + c.getId() + "/mensalidades", fabrica.responsavel())
                .andExpect(status().isForbidden());
        fazGet("/contratos/" + c.getId() + "/mensalidades", fabrica.motorista())
                .andExpect(status().isForbidden());
        fazGet("/contratos/" + c.getId() + "/mensalidades", fabrica.administrador())
                .andExpect(status().isForbidden());
        fazGet("/contratos/99999/mensalidades", resp).andExpect(status().isNotFound());
        fazGet("/contratos/" + c.getId() + "/mensalidades").andExpect(status().isUnauthorized());
    }

    @Test
    void listagemDoResponsavelTrazSoAsDeleEFiltraPorStatus() throws Exception {
        Contrato c = ativo();
        fabrica.mensalidade(c, OUTUBRO, StatusMensalidade.PAGA);
        fabrica.mensalidade(c, NOVEMBRO, StatusMensalidade.PENDENTE);
        Responsavel outro = fabrica.responsavel();
        fabrica.mensalidade(fabrica.contrato(outro, fabrica.aluno(outro, escola), atendimento, StatusContrato.ATIVO),
                NOVEMBRO, StatusMensalidade.PENDENTE);

        fazGet("/responsavel/mensalidades", resp)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].competencia").value("2026-11-01"));
        fazGet("/responsavel/mensalidades?status=PAGA", resp)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAGA"));
        fazGet("/responsavel/mensalidades?status=ATRASADA", resp)
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listagemDoResponsavelRecusaOutroPapelEStatusInvalido() throws Exception {
        fazGet("/responsavel/mensalidades", mot)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        fazGet("/responsavel/mensalidades?status=XYZ", resp)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void cobrancasDoMotoristaTrazemSoAsDeleEAplicamOsFiltros() throws Exception {
        Contrato c = ativo();
        fabrica.mensalidade(c, OUTUBRO, StatusMensalidade.PAGA);
        fabrica.mensalidade(c, NOVEMBRO, StatusMensalidade.PENDENTE);
        Motorista outro = fabrica.motorista();
        Atendimento doOutro = fabrica.atendimento(outro, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        fabrica.mensalidade(fabrica.contrato(resp, fabrica.aluno(resp, escola), doOutro, StatusContrato.ATIVO),
                NOVEMBRO, StatusMensalidade.PENDENTE);

        fazGet("/motorista/cobrancas", mot)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        fazGet("/motorista/cobrancas?status=PENDENTE", mot)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].competencia").value("2026-11-01"));
        fazGet("/motorista/cobrancas?competencia=2026-10", mot)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAGA"));
        fazGet("/motorista/cobrancas?competencia=2026-10&status=PAGA", mot)
                .andExpect(jsonPath("$.length()").value(1));
        fazGet("/motorista/cobrancas?competencia=2026-10&status=PENDENTE", mot)
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cobrancasRecusamOutroPapelECompetenciaInvalida() throws Exception {
        fazGet("/motorista/cobrancas", resp)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        fazGet("/motorista/cobrancas?competencia=abc", mot)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    // ---------------------------------------------------------------- serviço para outros módulos

    @Test
    void existePendenteOuAtrasadaOlhaSoOContratoInformado() {
        Contrato c = ativo();
        assertThat(consulta.existePendenteOuAtrasada(c.getId())).isFalse();

        fabrica.mensalidade(c, SETEMBRO, StatusMensalidade.PAGA);
        assertThat(consulta.existePendenteOuAtrasada(c.getId())).isFalse();

        fabrica.mensalidade(c, OUTUBRO, StatusMensalidade.ATRASADA);
        assertThat(consulta.existePendenteOuAtrasada(c.getId())).isTrue();

        Contrato outro = contrato(StatusContrato.ATIVO);
        assertThat(consulta.existePendenteOuAtrasada(outro.getId())).isFalse();

        fabrica.mensalidade(outro, NOVEMBRO, StatusMensalidade.PENDENTE);
        assertThat(consulta.existePendenteOuAtrasada(outro.getId())).isTrue();
    }
}
