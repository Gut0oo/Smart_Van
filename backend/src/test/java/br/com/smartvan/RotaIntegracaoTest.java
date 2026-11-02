package br.com.smartvan;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.maps.MapsIndisponivelException;
import br.com.smartvan.maps.RotaOtimizada;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Notificacao;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Veiculo;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.RotaRepository;
import br.com.smartvan.suporte.FakeMapsProvider;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RotaIntegracaoTest extends TesteIntegracaoBase {

    private static final String GERAR = "/motorista/rotas/gerar";

    @Autowired ContratoRepository contratos;
    @Autowired RotaRepository rotas;
    @Autowired EntityManagerFactory emf;

    Motorista motorista;
    Escola escola;
    Atendimento atendimento; //aula 07:00, saída 06:00, teto 06:45
    Responsavel resp1;
    Responsavel resp2;
    Aluno aluno1;
    Aluno aluno2;
    List<Aluno> alunos; //por nome: a mesma ordem enviada ao MapsProvider
    LocalDate amanha;

    @BeforeEach
    void cenario() {
        amanha = relogio.hoje().plusDays(1);
        motorista = fabrica.motorista();
        escola = fabrica.escola();
        atendimento = fabrica.atendimento(motorista, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        fabrica.enderecoPartida(motorista);
        fabrica.veiculo(motorista, 15);
        resp1 = fabrica.responsavel();
        resp2 = fabrica.responsavel();
        aluno1 = alunoComContrato(resp1);
        aluno2 = alunoComContrato(resp2);
        alunos = new ArrayList<>(List.of(aluno1, aluno2));
        alunos.sort(Comparator.comparing(Aluno::getNome).thenComparing(Aluno::getId));
    }

    private Aluno alunoComContrato(Responsavel r) {
        Aluno a = fabrica.aluno(r, escola);
        fabrica.contrato(r, a, atendimento, StatusContrato.ATIVO);
        return a;
    }

    private Map<String, Object> corpo(String tipo) {
        Map<String, Object> m = new HashMap<>();
        m.put("data", amanha.toString());
        m.put("tipo", tipo);
        m.put("atendimentoId", atendimento.getId());
        return m;
    }

    private Map<String, Object> corpoComSaida(String tipo, String horarioSaida) {
        Map<String, Object> m = corpo(tipo);
        m.put("horarioSaida", horarioSaida);
        return m;
    }

    private ResultActions gerar(Map<String, Object> corpo) throws Exception {
        return fazPost(GERAR, corpo, motorista);
    }

    private long idDe(ResultActions resposta) throws Exception {
        return json.readTree(resposta.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    // ---------------------------------------------------------------- ida e volta

    @Test
    void geraRotaDeIdaComASaidaDoAtendimento() throws Exception {
        gerar(corpo("IDA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLANEJADA"))
                .andExpect(jsonPath("$.tipo").value("IDA"))
                .andExpect(jsonPath("$.data").value(amanha.toString()))
                .andExpect(jsonPath("$.atendimentoId").value(atendimento.getId()))
                .andExpect(jsonPath("$.escola.id").value(escola.getId()))
                .andExpect(jsonPath("$.escola.nome").value(escola.getNome()))
                .andExpect(jsonPath("$.veiculo.placa").isString())
                .andExpect(jsonPath("$.veiculo.modelo").value("Van Teste"))
                .andExpect(jsonPath("$.otimizada").value(true))
                .andExpect(jsonPath("$.polyline").value("fake-polyline"))
                .andExpect(jsonPath("$.horarioInicio").value("06:00"))
                .andExpect(jsonPath("$.horarioFim").value("06:15"))
                .andExpect(jsonPath("$.tempoEstimadoMinutos").value(15))
                .andExpect(jsonPath("$.distanciaKm").value(6.0))
                .andExpect(jsonPath("$.foraDoTeto").value(false))
                .andExpect(jsonPath("$.minutosAdiantar").value(nullValue()))
                .andExpect(jsonPath("$.paradas.length()").value(3))
                .andExpect(jsonPath("$.paradas[0].ordem").value(1))
                .andExpect(jsonPath("$.paradas[0].tipoParada").value("EMBARQUE"))
                .andExpect(jsonPath("$.paradas[0].aluno.id").value(alunos.get(0).getId()))
                .andExpect(jsonPath("$.paradas[0].endereco.id").isNumber())
                .andExpect(jsonPath("$.paradas[0].horarioPrevisto").value("06:05"))
                .andExpect(jsonPath("$.paradas[1].tipoParada").value("EMBARQUE"))
                .andExpect(jsonPath("$.paradas[1].aluno.id").value(alunos.get(1).getId()))
                .andExpect(jsonPath("$.paradas[1].horarioPrevisto").value("06:10"))
                .andExpect(jsonPath("$.paradas[2].ordem").value(3))
                .andExpect(jsonPath("$.paradas[2].tipoParada").value("DESEMBARQUE"))
                .andExpect(jsonPath("$.paradas[2].aluno").value(nullValue()))
                .andExpect(jsonPath("$.paradas[2].endereco.id").value(escola.getEndereco().getId()))
                .andExpect(jsonPath("$.paradas[2].horarioPrevisto").value("06:15"));

        assertThat(maps.otimizacoes).hasSize(1);
        //IDA: parte do endereço do motorista e termina na escola
        assertThat(maps.otimizacoes.getFirst().partida().latitude()).isEqualByComparingTo("-23.563");
        assertThat(maps.otimizacoes.getFirst().destino().latitude()).isEqualByComparingTo("-23.56");
        assertThat(maps.otimizacoes.getFirst().paradas()).hasSize(2);
    }

    @Test
    void geraRotaDeVoltaSaindoDaEscolaAs1230() throws Exception {
        gerar(corpo("VOLTA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("VOLTA"))
                .andExpect(jsonPath("$.horarioInicio").value("12:30"))
                .andExpect(jsonPath("$.horarioFim").value("12:45"))
                .andExpect(jsonPath("$.paradas.length()").value(3))
                .andExpect(jsonPath("$.paradas[0].tipoParada").value("EMBARQUE"))
                .andExpect(jsonPath("$.paradas[0].aluno").value(nullValue()))
                .andExpect(jsonPath("$.paradas[0].endereco.id").value(escola.getEndereco().getId()))
                .andExpect(jsonPath("$.paradas[0].horarioPrevisto").value("12:30"))
                .andExpect(jsonPath("$.paradas[1].tipoParada").value("DESEMBARQUE"))
                .andExpect(jsonPath("$.paradas[1].aluno.id").value(alunos.get(0).getId()))
                .andExpect(jsonPath("$.paradas[1].horarioPrevisto").value("12:35"))
                .andExpect(jsonPath("$.paradas[2].tipoParada").value("DESEMBARQUE"))
                .andExpect(jsonPath("$.paradas[2].aluno.id").value(alunos.get(1).getId()))
                .andExpect(jsonPath("$.paradas[2].horarioPrevisto").value("12:40"));

        //VOLTA: parte da escola e termina no endereço do motorista
        assertThat(maps.otimizacoes.getFirst().partida().latitude()).isEqualByComparingTo("-23.56");
        assertThat(maps.otimizacoes.getFirst().destino().latitude()).isEqualByComparingTo("-23.563");
    }

    @Test
    void horarioDeSaidaInformadoPrevaleceNaIdaENaVolta() throws Exception {
        gerar(corpoComSaida("IDA", "05:50"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horarioInicio").value("05:50"))
                .andExpect(jsonPath("$.paradas[0].horarioPrevisto").value("05:55"))
                .andExpect(jsonPath("$.paradas[1].horarioPrevisto").value("06:00"))
                .andExpect(jsonPath("$.paradas[2].horarioPrevisto").value("06:05"));
        gerar(corpoComSaida("VOLTA", "13:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horarioInicio").value("13:00"));
    }

    @Test
    void saidaEnviadaAoProviderEstaNoFusoDeSaoPaulo() throws Exception {
        gerar(corpo("IDA")).andExpect(status().isCreated());

        //06:00 em São Paulo (UTC-3)
        assertThat(maps.otimizacoes.getFirst().saida().toString()).isEqualTo(amanha + "T09:00:00Z");
    }

    @Test
    void usaOrdemTemposEPolylineDoProvider() throws Exception {
        maps.otimizarComo((partida, paradas, destino, saida) ->
                new RotaOtimizada(List.of(1, 0), List.of(600, 1200), 1800, 9000, "abc123"));

        gerar(corpo("IDA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otimizada").value(true))
                .andExpect(jsonPath("$.polyline").value("abc123"))
                .andExpect(jsonPath("$.tempoEstimadoMinutos").value(30))
                .andExpect(jsonPath("$.distanciaKm").value(9.0))
                .andExpect(jsonPath("$.horarioFim").value("06:30"))
                .andExpect(jsonPath("$.paradas[0].aluno.id").value(alunos.get(1).getId()))
                .andExpect(jsonPath("$.paradas[0].horarioPrevisto").value("06:10"))
                .andExpect(jsonPath("$.paradas[1].aluno.id").value(alunos.get(0).getId()))
                .andExpect(jsonPath("$.paradas[1].horarioPrevisto").value("06:20"));
    }

    // ---------------------------------------------------------------- plano B

    @Test
    void planoBQuandoOProviderFalhaOrdenaPorProximidade() throws Exception {
        Motorista m = fabrica.motorista();
        Atendimento a = fabrica.atendimento(m, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        fabrica.enderecoPartida(m); //(-23.563, -46.697)
        fabrica.veiculo(m, 15);
        Aluno longe = fabrica.aluno(resp1, escola, fabrica.endereco("-23.6000000", "-46.8000000"));
        Aluno perto = fabrica.aluno(resp1, escola, fabrica.endereco("-23.5650000", "-46.6990000"));
        fabrica.contrato(resp1, longe, a, StatusContrato.ATIVO);
        fabrica.contrato(resp1, perto, a, StatusContrato.ATIVO);
        maps.falharNaOtimizacao();

        fazPost(GERAR, Map.of("data", amanha.toString(), "tipo", "IDA", "atendimentoId", a.getId()), m)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otimizada").value(false))
                .andExpect(jsonPath("$.polyline").value(nullValue()))
                .andExpect(jsonPath("$.tempoEstimadoMinutos").isNumber())
                .andExpect(jsonPath("$.distanciaKm").isNumber())
                .andExpect(jsonPath("$.paradas[0].aluno.id").value(perto.getId()))
                .andExpect(jsonPath("$.paradas[0].horarioPrevisto").isString())
                .andExpect(jsonPath("$.paradas[1].aluno.id").value(longe.getId()))
                .andExpect(jsonPath("$.paradas[2].tipoParada").value("DESEMBARQUE"));
    }

    @Test
    void planoBQuandoOProviderLancaOutraExcecao() throws Exception {
        maps.otimizarComo((p, ps, d, s) -> {
            throw new IllegalStateException("inesperado");
        });

        gerar(corpo("IDA")).andExpect(status().isCreated()).andExpect(jsonPath("$.otimizada").value(false));
    }

    @Test
    void planoBQuandoOProviderDevolveVazio() throws Exception {
        maps.otimizarComo((p, ps, d, s) -> new RotaOtimizada(List.of(), List.of(), 0, 0, null));

        gerar(corpo("IDA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otimizada").value(false))
                .andExpect(jsonPath("$.paradas.length()").value(3));
    }

    @Test
    void planoBQuandoOProviderDevolveNulo() throws Exception {
        maps.otimizarComo((p, ps, d, s) -> null);

        gerar(corpo("IDA")).andExpect(status().isCreated()).andExpect(jsonPath("$.otimizada").value(false));
    }

    @Test
    void planoBQuandoOProviderDevolveOrdemInvalida() throws Exception {
        maps.otimizarComo((p, ps, d, s) -> new RotaOtimizada(List.of(0, 0), List.of(1, 2), 3, 4, "x"));

        gerar(corpo("IDA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otimizada").value(false))
                .andExpect(jsonPath("$.polyline").value(nullValue()));
    }

    // ---------------------------------------------------------------- alunos da rota

    @Test
    void alunoComFaltaNaDataFicaForaDaRotaESemNotificacao() throws Exception {
        fabrica.falta(aluno1, amanha);

        gerar(corpo("IDA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paradas.length()").value(2))
                .andExpect(jsonPath("$.paradas[0].aluno.id").value(aluno2.getId()));

        assertThat(notificacoesDe(resp1, TipoNotificacao.ROTA_GERADA)).isEmpty();
        assertThat(notificacoesDe(resp2, TipoNotificacao.ROTA_GERADA)).hasSize(1);
    }

    @Test
    void faltaEmOutroDiaNaoTiraOAluno() throws Exception {
        fabrica.falta(aluno1, amanha.plusDays(1));

        gerar(corpo("IDA")).andExpect(status().isCreated()).andExpect(jsonPath("$.paradas.length()").value(3));
    }

    @Test
    void soEntramContratosAtivosDaqueleAtendimento() throws Exception {
        Aluno pendente = fabrica.aluno(resp1, escola);
        fabrica.contrato(resp1, pendente, atendimento, StatusContrato.PENDENTE);
        Aluno encerrado = fabrica.aluno(resp1, escola);
        fabrica.contrato(resp1, encerrado, atendimento, StatusContrato.ENCERRADO);
        //mesma escola e mesmo motorista, mas outro turno
        Atendimento tarde = fabrica.atendimento(motorista, escola, LocalTime.of(13, 0), LocalTime.of(12, 0));
        Aluno daTarde = fabrica.aluno(resp1, escola);
        fabrica.contrato(resp1, daTarde, tarde, StatusContrato.ATIVO);

        gerar(corpo("IDA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paradas.length()").value(3))
                .andExpect(jsonPath("$.paradas[0].aluno.id").value(alunos.get(0).getId()))
                .andExpect(jsonPath("$.paradas[1].aluno.id").value(alunos.get(1).getId()));
    }

    @Test
    void semAlunosDa422() throws Exception {
        fabrica.falta(aluno1, amanha);
        fabrica.falta(aluno2, amanha);

        gerar(corpo("IDA"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ROTA_SEM_ALUNOS"));
    }

    @Test
    void atendimentoSemContratosDa422() throws Exception {
        Atendimento vazio = fabrica.atendimento(motorista, escola, LocalTime.of(13, 0), LocalTime.of(12, 0));
        Map<String, Object> c = corpo("IDA");
        c.put("atendimentoId", vazio.getId());

        gerar(c).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("ROTA_SEM_ALUNOS"));
    }

    // ---------------------------------------------------------------- atendimento e pré-condições

    @Test
    void atendimentoInexistenteDa404() throws Exception {
        Map<String, Object> c = corpo("IDA");
        c.put("atendimentoId", 999999);

        gerar(c).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void atendimentoDeOutroMotoristaDa403() throws Exception {
        fazPost(GERAR, corpo("IDA"), fabrica.motorista())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void semEnderecoDePartidaDa422() throws Exception {
        Motorista sem = fabrica.motorista();
        fabrica.veiculo(sem, 10);
        Atendimento a = fabrica.atendimento(sem, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        fabrica.contrato(resp1, fabrica.aluno(resp1, escola), a, StatusContrato.ATIVO);

        fazPost(GERAR, Map.of("data", amanha.toString(), "tipo", "IDA", "atendimentoId", a.getId()), sem)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ENDERECO_PARTIDA_OBRIGATORIO"));
    }

    @Test
    void enderecoSemCoordenadasQueNaoGeocodificaDa422() throws Exception {
        fabrica.contrato(resp1, fabrica.aluno(resp1, escola, fabrica.endereco(null, null)), atendimento,
                StatusContrato.ATIVO);
        maps.naoGeocodificar();

        gerar(corpo("IDA"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ENDERECO_SEM_COORDENADAS"));
    }

    @Test
    void geocodificacaoIndisponivelNaRotaDa422() throws Exception {
        fabrica.contrato(resp1, fabrica.aluno(resp1, escola, fabrica.endereco(null, null)), atendimento,
                StatusContrato.ATIVO);
        maps.geocodificarComo(t -> {
            throw new MapsIndisponivelException("falha simulada no teste");
        });

        gerar(corpo("IDA"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ENDERECO_SEM_COORDENADAS"));
    }

    @Test
    void enderecoSemCoordenadasQueGeocodificaEUsadoEGravado() throws Exception {
        Aluno a = fabrica.aluno(resp1, escola, fabrica.endereco(null, null));
        fabrica.contrato(resp1, a, atendimento, StatusContrato.ATIVO);

        gerar(corpo("IDA")).andExpect(status().isCreated()).andExpect(jsonPath("$.paradas.length()").value(4));
        assertThat(maps.geocodificacoes).hasSize(1);

        //a coordenada ficou gravada: gerar a volta não geocodifica de novo
        gerar(corpo("VOLTA")).andExpect(status().isCreated());
        assertThat(maps.geocodificacoes).hasSize(1);
    }

    @Test
    void mapsForaDoArNaoImpedeSalvarOEndereco() throws Exception {
        maps.geocodificarComo(t -> {
            throw new MapsIndisponivelException("falha simulada no teste");
        });

        fazPut("/motorista/endereco-partida", Map.of("logradouro", "Rua A", "numero", "10", "bairro", "Centro",
                "cidade", "São Paulo", "uf", "SP", "cep", "01001000"), motorista)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logradouro").value("Rua A"))
                .andExpect(jsonPath("$.latitude").doesNotExist());
    }

    @Test
    void dataPassadaDa422EHojePode() throws Exception {
        Map<String, Object> c = corpo("IDA");
        c.put("data", relogio.hoje().minusDays(1).toString());
        gerar(c).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("DATA_PASSADA"));

        c.put("data", relogio.hoje().toString());
        gerar(c).andExpect(status().isCreated());
    }

    @Test
    void corpoInvalidoDa400() throws Exception {
        gerar(Map.of("data", amanha.toString(), "tipo", "IDA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.atendimentoId").isString());
    }

    @Test
    void responsavelNaoGeraNemConsultaRota() throws Exception {
        fazPost(GERAR, corpo("IDA"), resp1).andExpect(status().isForbidden());
        fazGet("/motorista/rotas", resp1).andExpect(status().isForbidden());
    }

    @Test
    void semIdentificacaoDa401() throws Exception {
        fazPost(GERAR, corpo("IDA")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- veículo

    @Test
    void semVeiculoDa422() throws Exception {
        Motorista sem = fabrica.motorista();
        fabrica.enderecoPartida(sem);
        Atendimento a = fabrica.atendimento(sem, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        fabrica.contrato(resp1, fabrica.aluno(resp1, escola), a, StatusContrato.ATIVO);

        fazPost(GERAR, Map.of("data", amanha.toString(), "tipo", "IDA", "atendimentoId", a.getId()), sem)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("VEICULO_OBRIGATORIO"));
    }

    @Test
    void variosVeiculosSemEscolherDa422EComIdFunciona() throws Exception {
        Veiculo segundo = fabrica.veiculo(motorista, 12);

        gerar(corpo("IDA"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("VEICULO_AMBIGUO"));

        Map<String, Object> c = corpo("IDA");
        c.put("veiculoId", segundo.getId());
        gerar(c).andExpect(status().isCreated()).andExpect(jsonPath("$.veiculo.id").value(segundo.getId()));
    }

    @Test
    void veiculoDeOutroMotoristaDa403() throws Exception {
        Map<String, Object> c = corpo("IDA");
        c.put("veiculoId", fabrica.veiculo(fabrica.motorista(), 10).getId());

        gerar(c).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void veiculoInexistenteDa404() throws Exception {
        Map<String, Object> c = corpo("IDA");
        c.put("veiculoId", 999999);

        gerar(c).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    // ---------------------------------------------------------------- reaproveitamento

    @Test
    void reaproveitaRotaComOMesmoConjuntoDeAlunosSemNotificarDeNovo() throws Exception {
        long id = idDe(gerar(corpo("IDA")).andExpect(status().isCreated()));

        gerar(corpo("IDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.paradas.length()").value(3))
                .andExpect(jsonPath("$.paradas[2].ordem").value(3));

        assertThat(maps.otimizacoes).hasSize(1);
        assertThat(rotas.count()).isEqualTo(1);
        assertThat(notificacoesDe(resp1, TipoNotificacao.ROTA_GERADA)).hasSize(1);
    }

    @Test
    void conjuntoDiferenteApagaAAntigaEGeraOutra() throws Exception {
        long antiga = idDe(gerar(corpo("IDA")));
        fabrica.falta(aluno1, amanha);

        long nova = idDe(gerar(corpo("IDA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paradas.length()").value(2)));

        assertThat(nova).isNotEqualTo(antiga);
        assertThat(rotas.count()).isEqualTo(1);
        fazGet("/motorista/rotas/" + antiga, motorista).andExpect(status().isNotFound());
    }

    @Test
    void idaEVoltaSaoRotasDiferentes() throws Exception {
        gerar(corpo("IDA")).andExpect(status().isCreated());
        gerar(corpo("VOLTA")).andExpect(status().isCreated());

        fazGet("/motorista/rotas?data=" + amanha, motorista)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("IDA"))
                .andExpect(jsonPath("$[1].tipo").value("VOLTA"));
    }

    @Test
    void atendimentosDaMesmaEscolaTemRotasSeparadasMesmoComOMesmoAluno() throws Exception {
        //o conjunto só é comparado dentro do mesmo atendimento: aqui o aluno 1 troca de turno entre as gerações
        long manha = idDe(gerar(corpo("IDA")).andExpect(status().isCreated()));
        Atendimento tarde = fabrica.atendimento(motorista, escola, LocalTime.of(13, 0), LocalTime.of(12, 0));
        for (Contrato c : contratos.findAll()) {
            c.setAtendimento(tarde);
            contratos.save(c);
        }
        Map<String, Object> c = corpo("IDA");
        c.put("atendimentoId", tarde.getId());

        long daTarde = idDe(gerar(c).andExpect(status().isCreated()));

        assertThat(daTarde).isNotEqualTo(manha);
        assertThat(rotas.count()).isEqualTo(2);
    }

    @Test
    void geracoesSimultaneasCriamUmaRotaSo() throws Exception {
        //sem a trava, as duas chegam juntas aqui e cada uma grava a sua rota. Com a trava, a segunda fica esperando,
        //a espera da primeira estoura e ela segue sozinha; a segunda encontra a rota pronta e reaproveita
        CyclicBarrier encontro = new CyclicBarrier(2);
        maps.otimizarComo((p, paradas, d, s) -> {
            try {
                encontro.await(500, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | BrokenBarrierException | InterruptedException e) {
                //a outra geração não chegou até aqui
            }
            return FakeMapsProvider.ordem(IntStream.range(0, paradas.size()).boxed().toList());
        });

        List<Integer> statusHttp = simultaneos(List.of(corpo("IDA"), corpo("IDA")));

        assertThat(statusHttp).containsExactlyInAnyOrder(201, 200);
        assertThat(rotas.count()).isEqualTo(1);
        assertThat(notificacoesDe(resp1, TipoNotificacao.ROTA_GERADA)).hasSize(1);
    }

    private List<Integer> simultaneos(List<Map<String, Object>> corpos) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(corpos.size());
        List<Integer> statusHttp = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> tarefas = new ArrayList<>();
            for (Map<String, Object> corpo : corpos) {
                tarefas.add(pool.submit(() -> {
                    largada.await();
                    statusHttp.add(gerar(corpo).andReturn().getResponse().getStatus());
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> t : tarefas) {
                t.get();
            }
        } finally {
            pool.shutdownNow();
        }
        return statusHttp;
    }

    // ---------------------------------------------------------------- teto de chegada

    @Test
    void chegadaDepoisDoTetoGeraARotaMarcadaENotificaOMotorista() throws Exception {
        //saída 06:40 + 15 min = 06:55; teto 06:45
        long id = idDe(gerar(corpoComSaida("IDA", "06:40"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foraDoTeto").value(true))
                .andExpect(jsonPath("$.minutosAdiantar").value(10)));

        List<Notificacao> avisos = notificacoesDe(motorista, TipoNotificacao.ROTA_GERADA);
        assertThat(avisos).hasSize(1);
        assertThat(avisos.getFirst().getReferenciaId()).isEqualTo(id);
        assertThat(avisos.getFirst().getTitulo()).isEqualTo("Rota fora do teto de chegada");
        assertThat(avisos.getFirst().getMensagem()).contains("06:55", "06:45", "10 minuto");

        fazGet("/motorista/rotas/" + id, motorista)
                .andExpect(jsonPath("$.foraDoTeto").value(true))
                .andExpect(jsonPath("$.minutosAdiantar").value(10));
    }

    @Test
    void chegadaExatamenteNoTetoEstaDentro() throws Exception {
        gerar(corpoComSaida("IDA", "06:30")) //06:45
                .andExpect(jsonPath("$.foraDoTeto").value(false))
                .andExpect(jsonPath("$.minutosAdiantar").value(nullValue()));

        assertThat(notificacoesDe(motorista, TipoNotificacao.ROTA_GERADA)).isEmpty();
    }

    @Test
    void segundosAcimaDoTetoArredondamParaCima() throws Exception {
        maps.otimizarComo((p, ps, d, s) -> new RotaOtimizada(List.of(0, 1), List.of(300, 600), 930, 6000, "x"));

        gerar(corpoComSaida("IDA", "06:30")) //06:45:30
                .andExpect(jsonPath("$.foraDoTeto").value(true))
                .andExpect(jsonPath("$.minutosAdiantar").value(1));
    }

    @Test
    void avisoDeForaDoTetoNaoSeRepeteNoReaproveitamento() throws Exception {
        gerar(corpoComSaida("IDA", "06:40")).andExpect(status().isCreated());

        gerar(corpoComSaida("IDA", "06:40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foraDoTeto").value(true))
                .andExpect(jsonPath("$.minutosAdiantar").value(10));

        assertThat(notificacoesDe(motorista, TipoNotificacao.ROTA_GERADA)).hasSize(1);
    }

    @Test
    void voltaNaoTemTeto() throws Exception {
        gerar(corpoComSaida("VOLTA", "18:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foraDoTeto").value(false))
                .andExpect(jsonPath("$.minutosAdiantar").value(nullValue()));

        assertThat(notificacoesDe(motorista, TipoNotificacao.ROTA_GERADA)).isEmpty();
    }

    @Test
    void estimativaDoPlanoBTambemMarcaForaDoTeto() throws Exception {
        maps.falharNaOtimizacao();

        //06:45 + qualquer deslocamento passa do teto; as casas e a escola estão a mais de 1 km da partida
        gerar(corpoComSaida("IDA", "06:45"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.otimizada").value(false))
                .andExpect(jsonPath("$.foraDoTeto").value(true))
                .andExpect(jsonPath("$.minutosAdiantar").isNumber());

        assertThat(notificacoesDe(motorista, TipoNotificacao.ROTA_GERADA)).hasSize(1);
    }

    // ---------------------------------------------------------------- remoção do atendimento

    private void encerrarContratos() {
        for (Contrato c : contratos.findAll()) {
            c.setStatus(StatusContrato.ENCERRADO);
            contratos.save(c);
        }
    }

    @Test
    void removerAtendimentoDesvinculaARotaSemErro() throws Exception {
        long id = idDe(gerar(corpo("IDA")).andExpect(status().isCreated()));
        encerrarContratos();

        fazPut("/motorista/escolas", Map.of("atendimentos", List.of()), motorista).andExpect(status().isOk());

        fazGet("/motorista/rotas/" + id, motorista)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentoId").value(nullValue()))
                .andExpect(jsonPath("$.escola.id").value(escola.getId()))
                .andExpect(jsonPath("$.paradas.length()").value(3));
    }

    @Test
    void rotaSemAtendimentoNaoEReaproveitada() throws Exception {
        long antiga = idDe(gerar(corpo("IDA")).andExpect(status().isCreated()));
        encerrarContratos();
        fazPut("/motorista/escolas", Map.of("atendimentos", List.of()), motorista).andExpect(status().isOk());

        //mesmos alunos, mesma escola e mesma data, em um turno novo
        Atendimento novo = fabrica.atendimento(motorista, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        fabrica.contrato(resp1, aluno1, novo, StatusContrato.ATIVO);
        fabrica.contrato(resp2, aluno2, novo, StatusContrato.ATIVO);
        Map<String, Object> c = corpo("IDA");
        c.put("atendimentoId", novo.getId());

        long nova = idDe(gerar(c)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atendimentoId").value(novo.getId())));

        assertThat(nova).isNotEqualTo(antiga);
        assertThat(rotas.count()).isEqualTo(2);
    }

    // ---------------------------------------------------------------- consultas

    @Test
    void listaPadraoEHojeEBuscaPorId() throws Exception {
        fazGet("/motorista/rotas", motorista).andExpect(jsonPath("$.length()").value(0));
        Map<String, Object> c = corpo("IDA");
        c.put("data", relogio.hoje().toString());
        long id = idDe(gerar(c).andExpect(status().isCreated()));

        fazGet("/motorista/rotas", motorista)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].paradas[0].ordem").value(1))
                .andExpect(jsonPath("$[0].paradas[2].ordem").value(3));
        fazGet("/motorista/rotas?data=" + amanha, motorista).andExpect(jsonPath("$.length()").value(0));
        fazGet("/motorista/rotas/" + id, motorista)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentoId").value(atendimento.getId()))
                .andExpect(jsonPath("$.paradas.length()").value(3));
    }

    @Test
    void motoristaSoVeASuaRota() throws Exception {
        long id = idDe(gerar(corpo("IDA")));
        Motorista outro = fabrica.motorista();

        fazGet("/motorista/rotas/" + id, outro).andExpect(status().isForbidden());
        fazGet("/motorista/rotas?data=" + amanha, outro).andExpect(jsonPath("$.length()").value(0));
        fazGet("/motorista/rotas/999999", motorista).andExpect(status().isNotFound());
    }

    @Test
    void listarEBuscarNaoFazemConsultaPorParada() throws Exception {
        long id = idDe(gerar(corpo("IDA")));
        gerar(corpo("VOLTA"));
        long comPoucos = comandosSql("/motorista/rotas?data=" + amanha);
        long buscaComPoucos = comandosSql("/motorista/rotas/" + id);

        alunoComContrato(resp1);
        alunoComContrato(resp1);
        alunoComContrato(resp2);
        id = idDe(gerar(corpo("IDA")).andExpect(status().isCreated()));
        gerar(corpo("VOLTA")).andExpect(status().isCreated());

        assertThat(comandosSql("/motorista/rotas?data=" + amanha)).isEqualTo(comPoucos);
        assertThat(comandosSql("/motorista/rotas/" + id)).isEqualTo(buscaComPoucos);
        //o usuário do interceptor e a consulta da rota
        assertThat(buscaComPoucos).isEqualTo(2);
    }

    private long comandosSql(String url) throws Exception {
        Statistics estatisticas = emf.unwrap(SessionFactory.class).getStatistics();
        estatisticas.setStatisticsEnabled(true);
        try {
            estatisticas.clear();
            fazGet(url, motorista).andExpect(status().isOk());
            return estatisticas.getPrepareStatementCount();
        } finally {
            estatisticas.setStatisticsEnabled(false);
        }
    }

    // ---------------------------------------------------------------- notificações

    @Test
    void notificaCadaResponsavelComOHorarioPrevistoDoSeuAluno() throws Exception {
        long id = idDe(gerar(corpo("IDA")));

        Responsavel doPrimeiro = alunos.getFirst() == aluno1 ? resp1 : resp2;
        Responsavel doSegundo = alunos.getFirst() == aluno1 ? resp2 : resp1;
        Notificacao primeira = notificacoesDe(doPrimeiro, TipoNotificacao.ROTA_GERADA).getFirst();
        Notificacao segunda = notificacoesDe(doSegundo, TipoNotificacao.ROTA_GERADA).getFirst();

        assertThat(primeira.getReferenciaId()).isEqualTo(id);
        assertThat(primeira.getMensagem()).contains("06:05", alunos.get(0).getNome());
        assertThat(segunda.getMensagem()).contains("06:10", alunos.get(1).getNome());
        assertThat(notificacoesDe(motorista, TipoNotificacao.ROTA_GERADA)).isEmpty();
    }

    @Test
    void falhaNaGeracaoNaoDeixaNotificacaoNemApagaARotaAntiga() throws Exception {
        long antiga = idDe(gerar(corpo("IDA")));
        int antes = notificacoesDe(resp1).size();
        //conjunto novo, com um endereço que não geocodifica: a transação inteira volta
        fabrica.contrato(resp1, fabrica.aluno(resp1, escola, fabrica.endereco(null, null)), atendimento,
                StatusContrato.ATIVO);
        maps.geocodificarComo(t -> Optional.empty());

        gerar(corpo("IDA")).andExpect(status().isUnprocessableContent());

        fazGet("/motorista/rotas/" + antiga, motorista).andExpect(status().isOk());
        assertThat(notificacoesDe(resp1)).hasSize(antes);
    }
}
