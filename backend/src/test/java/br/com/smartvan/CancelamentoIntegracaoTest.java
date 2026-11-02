package br.com.smartvan;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.StatusMensalidade;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.service.CancelamentoJob;
import br.com.smartvan.service.CancelamentoService;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CancelamentoIntegracaoTest extends TesteIntegracaoBase {

    private static final Map<String, String> CORPO = Map.of("motivo", "Mudança de cidade");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 3);
    private static final LocalDate NOVEMBRO = LocalDate.of(2026, 11, 1);
    private static final String SEM_ERRO = "SEM_ERRO";

    @Autowired CancelamentoJob job;
    @Autowired CancelamentoService service;
    @Autowired ContratoRepository contratos;
    @Autowired TransactionTemplate tx;

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

    private Contrato contrato(StatusContrato status) {
        return fabrica.contrato(resp, fabrica.aluno(resp, escola), atendimento, status);
    }

    private Contrato ativo() {
        return contrato(StatusContrato.ATIVO);
    }

    /** ATIVO, com cancelamento solicitado hoje: encerramento em 2026-12-03. */
    private Contrato comAviso() {
        return fabrica.comCancelamento(ativo(), TipoUsuario.RESPONSAVEL, HOJE);
    }

    private static String url(Contrato c) {
        return "/contratos/" + c.getId() + "/cancelamento";
    }

    private Contrato relido(Contrato c) {
        return contratos.findById(c.getId()).orElseThrow();
    }

    private StatusContrato statusDe(Contrato c) {
        return relido(c).getStatus();
    }

    private static UsuarioLogado logado(Usuario u) {
        return new UsuarioLogado(u.getId(), u.getTipo());
    }

    // ---------------------------------------------------------------- solicitação

    @Test
    void responsavelSolicitaEContratoContinuaAtivoComAvisoDe30Dias() throws Exception {
        Contrato c = ativo();

        fazPost(url(c), CORPO, resp)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.getId()))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.aluno.id").value(c.getAluno().getId()))
                .andExpect(jsonPath("$.responsavel.id").value(resp.getId()))
                .andExpect(jsonPath("$.motorista.id").value(mot.getId()))
                .andExpect(jsonPath("$.escola.id").value(escola.getId()))
                .andExpect(jsonPath("$.atendimento.id").value(atendimento.getId()))
                .andExpect(jsonPath("$.cancelamento.dataSolicitacao").value("2026-11-03"))
                .andExpect(jsonPath("$.cancelamento.dataEncerramentoEfetivo").value("2026-12-03"))
                .andExpect(jsonPath("$.cancelamento.solicitante").value("RESPONSAVEL"))
                .andExpect(jsonPath("$.cancelamento.motivo").value("Mudança de cidade"));

        Contrato gravado = relido(c);
        assertThat(gravado.getStatus()).isEqualTo(StatusContrato.ATIVO);
        assertThat(gravado.getDataSolicitacaoCancelamento()).isEqualTo(HOJE);
        assertThat(gravado.getDataEncerramentoEfetivo()).isEqualTo(LocalDate.of(2026, 12, 3));
        assertThat(gravado.getSolicitanteCancelamento()).isEqualTo(TipoUsuario.RESPONSAVEL);
        assertThat(gravado.getMotivoCancelamento()).isEqualTo("Mudança de cidade");
    }

    @Test
    void solicitacaoDoResponsavelNotificaSoOMotorista() throws Exception {
        Contrato c = ativo();

        fazPost(url(c), CORPO, resp).andExpect(status().isOk());

        assertThat(notificacoesDe(mot, TipoNotificacao.CANCELAMENTO_SOLICITADO)).hasSize(1);
        assertThat(notificacoesDe(mot).get(0).getReferenciaId()).isEqualTo(c.getId());
        assertThat(notificacoesDe(resp)).isEmpty();
    }

    @Test
    void solicitacaoDoMotoristaNotificaSoOResponsavel() throws Exception {
        Contrato c = ativo();

        fazPost(url(c), CORPO, mot)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelamento.solicitante").value("MOTORISTA"));

        assertThat(notificacoesDe(resp, TipoNotificacao.CANCELAMENTO_SOLICITADO)).hasSize(1);
        assertThat(notificacoesDe(mot)).isEmpty();
    }

    @Test
    void semIdentificacaoDa401() throws Exception {
        fazPost(url(ativo()), CORPO)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void quemNaoEParteDa403ENaoGravaNada() throws Exception {
        Contrato c = ativo();
        for (Usuario outro : List.of(fabrica.responsavel(), fabrica.motorista(), fabrica.administrador())) {
            fazPost(url(c), CORPO, outro)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        }
        assertThat(relido(c).getDataSolicitacaoCancelamento()).isNull();
        assertThat(notificacoesDe(mot)).isEmpty();
        assertThat(notificacoesDe(resp)).isEmpty();
    }

    @Test
    void quemNaoEParteRecebe403MesmoComContratoEmOutroEstado() throws Exception {
        fazPost(url(contrato(StatusContrato.ENCERRADO)), CORPO, fabrica.responsavel())
                .andExpect(status().isForbidden());
        fazPost(url(comAviso()), CORPO, fabrica.motorista())
                .andExpect(status().isForbidden());
    }

    @Test
    void contratoInexistenteDa404() throws Exception {
        fazPost("/contratos/999999/cancelamento", CORPO, resp)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void contratoQueNaoEstaAtivoDa409() throws Exception {
        for (StatusContrato s : List.of(StatusContrato.PENDENTE, StatusContrato.RECUSADO, StatusContrato.ENCERRADO)) {
            Contrato c = contrato(s);
            fazPost(url(c), CORPO, resp)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONTRATO_NAO_ATIVO"));
            assertThat(relido(c).getDataSolicitacaoCancelamento()).isNull();
        }
    }

    @Test
    void segundaSolicitacaoDa409EMantemAPrimeira() throws Exception {
        Contrato c = ativo();
        fazPost(url(c), CORPO, resp).andExpect(status().isOk());
        relogio.avancarDias(5);

        //também vale para a outra parte
        fazPost(url(c), Map.of("motivo", "Outro motivo"), mot)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CANCELAMENTO_JA_SOLICITADO"));

        Contrato gravado = relido(c);
        assertThat(gravado.getDataSolicitacaoCancelamento()).isEqualTo(HOJE);
        assertThat(gravado.getSolicitanteCancelamento()).isEqualTo(TipoUsuario.RESPONSAVEL);
        assertThat(gravado.getMotivoCancelamento()).isEqualTo("Mudança de cidade");
        assertThat(notificacoesDe(resp)).isEmpty();
    }

    @Test
    void motivoInvalidoDa400() throws Exception {
        Contrato c = ativo();
        List<Map<String, String>> corpos = List.of(Map.of(), Map.of("motivo", "1234"), Map.of("motivo", "      "),
                Map.of("motivo", "x".repeat(501)));
        for (Map<String, String> corpo : corpos) {
            fazPost(url(c), corpo, resp)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                    .andExpect(jsonPath("$.campos.motivo").exists());
        }
        assertThat(relido(c).getDataSolicitacaoCancelamento()).isNull();
    }

    @Test
    void motivoNosLimitesDe5E500Passa() throws Exception {
        fazPost(url(ativo()), Map.of("motivo", "12345"), resp).andExpect(status().isOk());
        fazPost(url(ativo()), Map.of("motivo", "x".repeat(500)), resp)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelamento.motivo").value("x".repeat(500)));
    }

    // ---------------------------------------------------------------- agendador

    @Test
    void agendadorNaoEncerraNaVesperaDaData() {
        Contrato c = comAviso();
        relogio.definirData(LocalDate.of(2026, 12, 2));

        assertThat(job.encerrarVencidos()).isZero();

        assertThat(statusDe(c)).isEqualTo(StatusContrato.ATIVO);
        assertThat(notificacoesDe(resp)).isEmpty();
        assertThat(notificacoesDe(mot)).isEmpty();
    }

    @Test
    void agendadorEncerraNaDataESemPendenciaNotificaAsDuasPartes() {
        Contrato c = comAviso();
        fabrica.mensalidade(c, NOVEMBRO, StatusMensalidade.PAGA);
        relogio.definirData(LocalDate.of(2026, 12, 3));

        assertThat(job.encerrarVencidos()).isEqualTo(1);

        assertThat(statusDe(c)).isEqualTo(StatusContrato.ENCERRADO);
        for (Usuario parte : List.of(resp, mot)) {
            assertThat(notificacoesDe(parte, TipoNotificacao.CONTRATO_ENCERRADO)).hasSize(1);
            assertThat(notificacoesDe(parte, TipoNotificacao.CANCELAMENTO_COM_PENDENCIA)).isEmpty();
            assertThat(notificacoesDe(parte).get(0).getReferenciaId()).isEqualTo(c.getId());
        }
    }

    @Test
    void agendadorEncerraContratoComDataJaPassada() {
        Contrato c = comAviso();
        relogio.definirData(LocalDate.of(2026, 12, 20));

        assertThat(job.encerrarVencidos()).isEqualTo(1);

        assertThat(statusDe(c)).isEqualTo(StatusContrato.ENCERRADO);
    }

    @Test
    void mensalidadePendenteEncerraENotificaPendenciaAsDuasPartes() {
        encerraENotificaPendencia(StatusMensalidade.PENDENTE);
    }

    @Test
    void mensalidadeAtrasadaEncerraENotificaPendenciaAsDuasPartes() {
        encerraENotificaPendencia(StatusMensalidade.ATRASADA);
    }

    private void encerraENotificaPendencia(StatusMensalidade statusMensalidade) {
        Contrato c = comAviso();
        fabrica.mensalidade(c, NOVEMBRO, statusMensalidade);
        relogio.definirData(LocalDate.of(2026, 12, 3));

        assertThat(job.encerrarVencidos()).isEqualTo(1);

        assertThat(statusDe(c)).isEqualTo(StatusContrato.ENCERRADO);
        for (Usuario parte : List.of(resp, mot)) {
            assertThat(notificacoesDe(parte, TipoNotificacao.CANCELAMENTO_COM_PENDENCIA)).hasSize(1);
            assertThat(notificacoesDe(parte, TipoNotificacao.CONTRATO_ENCERRADO)).isEmpty();
        }
    }

    @Test
    void pendenciaDeOutroContratoNaoConta() {
        Contrato c = comAviso();
        fabrica.mensalidade(ativo(), NOVEMBRO, StatusMensalidade.PENDENTE);
        relogio.definirData(LocalDate.of(2026, 12, 3));

        job.encerrarVencidos();

        assertThat(statusDe(c)).isEqualTo(StatusContrato.ENCERRADO);
        assertThat(notificacoesDe(resp, TipoNotificacao.CONTRATO_ENCERRADO)).hasSize(1);
        assertThat(notificacoesDe(resp, TipoNotificacao.CANCELAMENTO_COM_PENDENCIA)).isEmpty();
    }

    @Test
    void agendadorIgnoraContratoSemSolicitacaoENaoAtivo() {
        Contrato semSolicitacao = ativo();
        Contrato pendente = fabrica.comCancelamento(contrato(StatusContrato.PENDENTE), TipoUsuario.RESPONSAVEL, HOJE);
        Contrato encerrado = fabrica.comCancelamento(contrato(StatusContrato.ENCERRADO), TipoUsuario.RESPONSAVEL, HOJE);
        relogio.definirData(LocalDate.of(2027, 3, 1));

        assertThat(job.encerrarVencidos()).isZero();

        assertThat(statusDe(semSolicitacao)).isEqualTo(StatusContrato.ATIVO);
        assertThat(statusDe(pendente)).isEqualTo(StatusContrato.PENDENTE);
        assertThat(statusDe(encerrado)).isEqualTo(StatusContrato.ENCERRADO);
        assertThat(notificacoesDe(resp)).isEmpty();
    }

    @Test
    void segundaExecucaoNaoRepeteEncerramentoNemNotificacao() {
        comAviso();
        relogio.definirData(LocalDate.of(2026, 12, 3));

        assertThat(job.encerrarVencidos()).isEqualTo(1);
        assertThat(job.encerrarVencidos()).isZero();

        assertThat(notificacoesDe(resp, TipoNotificacao.CONTRATO_ENCERRADO)).hasSize(1);
        assertThat(notificacoesDe(mot, TipoNotificacao.CONTRATO_ENCERRADO)).hasSize(1);
    }

    @Test
    void encerrarConfereDeNovoOStatusEAData() {
        //simula o estado mudar depois que o agendador leu a lista
        Contrato semSolicitacao = ativo();
        Contrato aindaNoPrazo = comAviso();
        LocalDate vencida = LocalDate.of(2026, 10, 1);
        Contrato encerradoVencido = fabrica.comCancelamento(contrato(StatusContrato.ENCERRADO), TipoUsuario.MOTORISTA, vencida);

        assertThat(service.encerrar(semSolicitacao.getId())).isFalse();
        assertThat(service.encerrar(aindaNoPrazo.getId())).isFalse();
        assertThat(service.encerrar(encerradoVencido.getId())).isFalse();

        assertThat(statusDe(semSolicitacao)).isEqualTo(StatusContrato.ATIVO);
        assertThat(statusDe(aindaNoPrazo)).isEqualTo(StatusContrato.ATIVO);
        assertThat(notificacoesDe(resp)).isEmpty();
        assertThat(notificacoesDe(mot)).isEmpty();
    }

    @Test
    void contratoEncerradoRecusaNovaSolicitacao() throws Exception {
        Contrato c = comAviso();
        relogio.definirData(LocalDate.of(2026, 12, 3));
        job.encerrarVencidos();

        fazPost(url(c), CORPO, mot)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONTRATO_NAO_ATIVO"));
    }

    // ---------------------------------------------------------------- concorrência

    @Test
    void segundaSolicitacaoEsperaAPrimeiraENaoSobrescreve() throws Exception {
        Contrato c = ativo();

        String codigo = segundaEsperaAPrimeira(
                () -> service.solicitar(logado(resp), c.getId(), "Motivo da primeira"),
                () -> service.solicitar(logado(mot), c.getId(), "Motivo da segunda"));

        assertThat(codigo).isEqualTo("CANCELAMENTO_JA_SOLICITADO");
        Contrato gravado = relido(c);
        assertThat(gravado.getMotivoCancelamento()).isEqualTo("Motivo da primeira");
        assertThat(gravado.getSolicitanteCancelamento()).isEqualTo(TipoUsuario.RESPONSAVEL);
        assertThat(notificacoesDe(mot, TipoNotificacao.CANCELAMENTO_SOLICITADO)).hasSize(1);
        assertThat(notificacoesDe(resp, TipoNotificacao.CANCELAMENTO_SOLICITADO)).isEmpty();
    }

    @Test
    void solicitacaoEsperaOEncerramentoEVeOContratoJaEncerrado() throws Exception {
        Contrato c = comAviso();
        relogio.definirData(LocalDate.of(2026, 12, 3));

        String codigo = segundaEsperaAPrimeira(
                () -> service.encerrar(c.getId()),
                () -> service.solicitar(logado(mot), c.getId(), "Motivo tardio"));

        assertThat(codigo).isEqualTo("CONTRATO_NAO_ATIVO");
        assertThat(statusDe(c)).isEqualTo(StatusContrato.ENCERRADO);
    }

    @Test
    void segundoEncerramentoEsperaOPrimeiroENaoNotificaDeNovo() throws Exception {
        Contrato c = comAviso();
        relogio.definirData(LocalDate.of(2026, 12, 3));

        String codigo = segundaEsperaAPrimeira(
                () -> service.encerrar(c.getId()),
                () -> service.encerrar(c.getId()));

        assertThat(codigo).isEqualTo(SEM_ERRO);
        assertThat(notificacoesDe(resp, TipoNotificacao.CONTRATO_ENCERRADO)).hasSize(1);
        assertThat(notificacoesDe(mot, TipoNotificacao.CONTRATO_ENCERRADO)).hasSize(1);
    }

    /**
     * Roda {@code primeira} numa transação aberta e só confirma depois que {@code segunda}, em outra thread, estiver
     * parada no banco (ou já tiver terminado). Assim a ordem não depende de sorte. Devolve o código do erro da segunda.
     */
    private String segundaEsperaAPrimeira(Runnable primeira, Callable<?> segunda) throws Exception {
        FutureTask<String> tarefa = new FutureTask<>(() -> {
            try {
                segunda.call();
                return SEM_ERRO;
            } catch (RegraNegocioException e) {
                return e.getCodigo();
            }
        });
        Thread thread = new Thread(tarefa);
        tx.executeWithoutResult(status -> {
            primeira.run();
            thread.start();
            esperarParadaNoBanco(thread);
        });
        return tarefa.get(10, TimeUnit.SECONDS);
    }

    //o limite fica abaixo do lock timeout do H2; se estourar, quem decide o teste é o resultado da segunda
    private static void esperarParadaNoBanco(Thread thread) {
        long limite = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(800);
        try {
            while (thread.isAlive() && !paradaNoBanco(thread) && System.nanoTime() < limite) {
                Thread.sleep(5);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean paradaNoBanco(Thread thread) {
        Thread.State estado = thread.getState();
        return (estado == Thread.State.WAITING || estado == Thread.State.TIMED_WAITING)
                && Arrays.stream(thread.getStackTrace()).anyMatch(f -> f.getClassName().startsWith("org.h2."));
    }
}
