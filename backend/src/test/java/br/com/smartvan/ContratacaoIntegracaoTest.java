package br.com.smartvan;

import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.event.ContratoAtivadoEvent;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class ContratacaoIntegracaoTest extends TesteIntegracaoBase {

    private static final LocalTime AULA = LocalTime.of(7, 0);
    private static final LocalTime SAIDA = LocalTime.of(6, 0);

    @Autowired ContratoRepository contratoRepository;
    @Autowired EntityManagerFactory emf;
    @Autowired ApplicationEvents eventos;

    /** Motorista VALIDADO com veículo da capacidade dada e um atendimento na escola. */
    private Atendimento atendimentoCom(Escola escola, int capacidade) {
        Motorista m = fabrica.motorista();
        fabrica.veiculo(m, capacidade);
        return fabrica.atendimento(m, escola, AULA, SAIDA);
    }

    private Responsavel responsavelComAluno(Escola escola) {
        Responsavel r = fabrica.responsavel();
        fabrica.aluno(r, escola);
        return r;
    }

    private Map<String, Object> proposta(Atendimento atendimento, Aluno aluno) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("atendimentoId", atendimento.getId());
        corpo.put("alunoId", aluno.getId());
        corpo.put("valorMensal", 350.00);
        return corpo;
    }

    private ResultActions propor(Responsavel r, Atendimento atendimento, Aluno aluno) throws Exception {
        return fazPost("/contratos", proposta(atendimento, aluno), r);
    }

    private Contrato pendente(Responsavel r, Aluno aluno, Atendimento atendimento) {
        return fabrica.contrato(r, aluno, atendimento, StatusContrato.PENDENTE);
    }

    private ResultActions aceitar(Contrato c, Motorista m) throws Exception {
        return fazPostSemCorpo("/contratos/" + c.getId() + "/aceitar", m);
    }

    private ResultActions recusar(Contrato c, Motorista m) throws Exception {
        return fazPostSemCorpo("/contratos/" + c.getId() + "/recusar", m);
    }

    private StatusContrato statusDe(Contrato c) {
        return contratoRepository.findById(c.getId()).orElseThrow().getStatus();
    }

    // ---------------------------------------------------------------- busca

    @Test
    void buscaSemEscolaDevolve400() throws Exception {
        Responsavel r = responsavelComAluno(fabrica.escola());
        fazGet("/motoristas", r).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void buscaComHorarioMalFormadoDevolve400() throws Exception {
        Escola e = fabrica.escola();
        fazGet("/motoristas?escolaId=" + e.getId() + "&horarioAula=25:00", responsavelComAluno(e))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void buscaSemAlunoDevolve422() throws Exception {
        Escola e = fabrica.escola();
        atendimentoCom(e, 10);
        fazGet("/motoristas?escolaId=" + e.getId(), fabrica.responsavel())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ALUNO_OBRIGATORIO"));
    }

    @Test
    void buscaExigeLoginEPapelDeResponsavel() throws Exception {
        Escola e = fabrica.escola();
        fazGet("/motoristas?escolaId=" + e.getId()).andExpect(status().isUnauthorized());
        fazGet("/motoristas?escolaId=" + e.getId(), fabrica.motorista()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void buscaDevolveOsAtendimentosQueCombinamComAsVagasDeCada() throws Exception {
        Escola e = fabrica.escola("Colégio Girassol");
        Motorista m = fabrica.motorista("Ana Motorista", StatusMotorista.VALIDADO);
        fabrica.veiculo(m, 3);
        Atendimento manha = fabrica.atendimento(m, e, AULA, SAIDA);
        Atendimento tarde = fabrica.atendimento(m, e, LocalTime.of(13, 0), LocalTime.of(12, 0));
        fabrica.atendimento(m, fabrica.escola(), AULA, SAIDA);
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, e), manha, StatusContrato.ATIVO);
        fabrica.contrato(r, fabrica.aluno(r, e), tarde, StatusContrato.PENDENTE);

        fazGet("/motoristas?escolaId=" + e.getId(), r).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(m.getId()))
                .andExpect(jsonPath("$[0].nome").value("Ana Motorista"))
                .andExpect(jsonPath("$[0].veiculo.capacidade").value(3))
                .andExpect(jsonPath("$[0].veiculo.tipo").value("VAN"))
                .andExpect(jsonPath("$[0].atendimentos.length()").value(2))
                .andExpect(jsonPath("$[0].atendimentos[0].id").value(manha.getId()))
                .andExpect(jsonPath("$[0].atendimentos[0].escola.nome").value("Colégio Girassol"))
                .andExpect(jsonPath("$[0].atendimentos[0].cidade").value("São Paulo"))
                .andExpect(jsonPath("$[0].atendimentos[0].horarioInicioAula").value("07:00"))
                .andExpect(jsonPath("$[0].atendimentos[0].horarioSaidaMotorista").value("06:00"))
                .andExpect(jsonPath("$[0].atendimentos[0].vagasRestantes").value(2))
                // proposta PENDENTE não ocupa vaga
                .andExpect(jsonPath("$[0].atendimentos[1].id").value(tarde.getId()))
                .andExpect(jsonPath("$[0].atendimentos[1].vagasRestantes").value(3));
    }

    @Test
    void vagasUsamOMaiorVeiculoDoMotorista() throws Exception {
        Escola e = fabrica.escola();
        Motorista m = fabrica.motorista();
        fabrica.veiculo(m, 2);
        fabrica.veiculo(m, 8);
        fabrica.veiculo(m, 4);
        fabrica.atendimento(m, e, AULA, SAIDA);

        fazGet("/motoristas?escolaId=" + e.getId(), responsavelComAluno(e))
                .andExpect(jsonPath("$[0].veiculo.capacidade").value(8))
                .andExpect(jsonPath("$[0].atendimentos[0].vagasRestantes").value(8));
    }

    @Test
    void buscaSoTrazMotoristaValidado() throws Exception {
        Escola e = fabrica.escola();
        Atendimento validado = atendimentoCom(e, 5);
        for (StatusMotorista s : List.of(StatusMotorista.PENDENTE, StatusMotorista.EM_ANALISE, StatusMotorista.REJEITADO)) {
            Motorista m = fabrica.motorista(s);
            fabrica.veiculo(m, 5);
            fabrica.atendimento(m, e, AULA, SAIDA);
        }

        fazGet("/motoristas?escolaId=" + e.getId(), responsavelComAluno(e))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(validado.getMotorista().getId()));
    }

    @Test
    void atendimentoLotadoNaoApareceMasOOutroDoMesmoMotoristaSim() throws Exception {
        Escola e = fabrica.escola();
        Motorista m = fabrica.motorista();
        fabrica.veiculo(m, 1);
        Atendimento lotado = fabrica.atendimento(m, e, AULA, SAIDA);
        Atendimento livre = fabrica.atendimento(m, e, LocalTime.of(13, 0), LocalTime.of(12, 0));
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, e), lotado, StatusContrato.ATIVO);

        fazGet("/motoristas?escolaId=" + e.getId(), r)
                .andExpect(jsonPath("$[0].atendimentos.length()").value(1))
                .andExpect(jsonPath("$[0].atendimentos[0].id").value(livre.getId()));
    }

    @Test
    void motoristaComTodosOsAtendimentosLotadosNaoAparece() throws Exception {
        Escola e = fabrica.escola();
        Atendimento lotado = atendimentoCom(e, 1);
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, e), lotado, StatusContrato.ATIVO);

        fazGet("/motoristas?escolaId=" + e.getId(), r).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void motoristaSemVeiculoNaoAparece() throws Exception {
        Escola e = fabrica.escola();
        fabrica.atendimento(fabrica.motorista(), e, AULA, SAIDA);

        fazGet("/motoristas?escolaId=" + e.getId(), responsavelComAluno(e))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void contratoEncerradoERecusadoNaoOcupamVaga() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 1);
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.ENCERRADO);
        fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.RECUSADO);

        fazGet("/motoristas?escolaId=" + e.getId(), r)
                .andExpect(jsonPath("$[0].atendimentos[0].vagasRestantes").value(1));
    }

    @Test
    void horarioAulaTemToleranciaDe30MinutosParaOsDoisLados() throws Exception {
        Escola e = fabrica.escola();
        atendimentoCom(e, 5);
        Responsavel r = responsavelComAluno(e);
        String url = "/motoristas?escolaId=" + e.getId() + "&horarioAula=";

        for (String dentro : List.of("07:00", "06:30", "07:30")) {
            fazGet(url + dentro, r).andExpect(jsonPath("$.length()").value(1));
        }
        for (String fora : List.of("06:29", "07:31")) {
            fazGet(url + fora, r).andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Test
    void toleranciaNaoAtravessaAMeiaNoite() throws Exception {
        Escola e = fabrica.escola();
        Motorista m = fabrica.motorista();
        fabrica.veiculo(m, 5);
        fabrica.atendimento(m, e, LocalTime.of(23, 50), LocalTime.of(23, 0));
        Responsavel r = responsavelComAluno(e);
        String url = "/motoristas?escolaId=" + e.getId() + "&horarioAula=";

        fazGet(url + "00:10", r).andExpect(jsonPath("$.length()").value(0));
        fazGet(url + "23:40", r).andExpect(jsonPath("$.length()").value(1));
        // perto da virada a janela vai só até o fim do dia, sem dar a volta
        fazGet(url + "23:45", r).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void filtrosDeCidadeENomeCombinamComAnd() throws Exception {
        Escola e = fabrica.escola("Colégio Sol", "Campinas", "-22.90", "-47.06");
        Motorista ana = fabrica.motorista("Ana Souza", StatusMotorista.VALIDADO);
        fabrica.veiculo(ana, 5);
        fabrica.atendimento(ana, e, AULA, SAIDA);
        Motorista bia = fabrica.motorista("Bia Lima", StatusMotorista.VALIDADO);
        fabrica.veiculo(bia, 5);
        fabrica.atendimento(bia, e, AULA, SAIDA);
        Responsavel r = responsavelComAluno(e);
        String url = "/motoristas?escolaId=" + e.getId();

        fazGet(url + "&nome=SOUZA", r).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ana.getId()));
        fazGet(url + "&cidade=campi", r).andExpect(jsonPath("$.length()").value(2));
        fazGet(url + "&cidade=campi&nome=bia", r).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(bia.getId()));
        fazGet(url + "&cidade=santos&nome=bia", r).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscaDeEscolaSemAtendimentoDevolveListaVazia() throws Exception {
        fazGet("/motoristas?escolaId=999999", responsavelComAluno(fabrica.escola()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long consultasDaBusca(String url, Responsavel r) throws Exception {
        Statistics estatisticas = emf.unwrap(SessionFactory.class).getStatistics();
        estatisticas.setStatisticsEnabled(true);
        estatisticas.clear();
        fazGet(url, r).andExpect(status().isOk());
        long consultas = estatisticas.getPrepareStatementCount();
        estatisticas.setStatisticsEnabled(false);
        return consultas;
    }

    @Test
    void buscaNaoFazConsultaPorMotorista() throws Exception {
        Escola e = fabrica.escola();
        atendimentoCom(e, 5);
        Responsavel r = responsavelComAluno(e);
        String url = "/motoristas?escolaId=" + e.getId();
        long comUm = consultasDaBusca(url, r);

        for (int i = 0; i < 5; i++) {
            atendimentoCom(e, 5);
        }
        fazGet(url, r).andExpect(jsonPath("$.length()").value(6));
        assertThat(consultasDaBusca(url, r)).isEqualTo(comUm);
    }

    // ---------------------------------------------------------------- pesquisa pelo nome

    @Test
    void semEscolaESemNomePreenchidoDevolve400() throws Exception {
        Responsavel r = responsavelComAluno(fabrica.escola());
        for (String url : List.of("/motoristas", "/motoristas?nome=", "/motoristas?nome=  ",
                "/motoristas?cidade=paulo", "/motoristas?horarioAula=07:00")) {
            fazGet(url, r).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        }
    }

    @Test
    void pesquisaPeloNomeSemEscolaTrazOsAtendimentosDeTodasAsEscolas() throws Exception {
        Motorista ana = fabrica.motorista("Ana Souza", StatusMotorista.VALIDADO);
        fabrica.veiculo(ana, 5);
        Atendimento escolaA = fabrica.atendimento(ana, fabrica.escola("Colégio A"), AULA, SAIDA);
        Atendimento escolaB = fabrica.atendimento(ana, fabrica.escola("Colégio B"), LocalTime.of(13, 0), LocalTime.of(12, 0));
        atendimentoCom(fabrica.escola(), 5);

        fazGet("/motoristas?nome=SOUZA", responsavelComAluno(fabrica.escola())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ana.getId()))
                .andExpect(jsonPath("$[0].atendimentos.length()").value(2))
                .andExpect(jsonPath("$[0].atendimentos[0].id").value(escolaA.getId()))
                .andExpect(jsonPath("$[0].atendimentos[1].id").value(escolaB.getId()));
    }

    @Test
    void pesquisaPeloNomeSemEscolaMostraAtendimentoLotadoComZeroVagas() throws Exception {
        Escola e = fabrica.escola();
        Motorista ana = fabrica.motorista("Ana Souza", StatusMotorista.VALIDADO);
        fabrica.veiculo(ana, 1);
        Atendimento lotado = fabrica.atendimento(ana, e, AULA, SAIDA);
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, e), lotado, StatusContrato.ATIVO);

        fazGet("/motoristas?nome=ana", r)
                .andExpect(jsonPath("$[0].atendimentos.length()").value(1))
                .andExpect(jsonPath("$[0].atendimentos[0].id").value(lotado.getId()))
                .andExpect(jsonPath("$[0].atendimentos[0].vagasRestantes").value(0));
    }

    @Test
    void pesquisaPeloNomeSemEscolaMostraMotoristaSemVeiculoComZeroVagas() throws Exception {
        Escola e = fabrica.escola();
        fabrica.atendimento(fabrica.motorista("Ana Souza", StatusMotorista.VALIDADO), e, AULA, SAIDA);

        fazGet("/motoristas?nome=ana", responsavelComAluno(e))
                .andExpect(jsonPath("$[0].veiculo").isEmpty())
                .andExpect(jsonPath("$[0].atendimentos[0].vagasRestantes").value(0));
    }

    @Test
    void comEscolaEComNomeAtendimentoLotadoContinuaForaDaLista() throws Exception {
        Escola e = fabrica.escola();
        Motorista ana = fabrica.motorista("Ana Souza", StatusMotorista.VALIDADO);
        fabrica.veiculo(ana, 1);
        Atendimento lotado = fabrica.atendimento(ana, e, AULA, SAIDA);
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, e), lotado, StatusContrato.ATIVO);

        fazGet("/motoristas?escolaId=" + e.getId() + "&nome=ana", r).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void pesquisaPeloNomeCombinaComCidadeEHorario() throws Exception {
        Escola campinas = fabrica.escola("Colégio Sol", "Campinas", "-22.90", "-47.06");
        Escola santos = fabrica.escola("Colégio Mar", "Santos", "-23.96", "-46.33");
        Motorista ana = fabrica.motorista("Ana Souza", StatusMotorista.VALIDADO);
        fabrica.veiculo(ana, 5);
        Atendimento emCampinas = fabrica.atendimento(ana, campinas, AULA, SAIDA);
        Atendimento emSantos = fabrica.atendimento(ana, santos, LocalTime.of(13, 0), LocalTime.of(12, 0));
        Responsavel r = responsavelComAluno(campinas);

        fazGet("/motoristas?nome=ana&cidade=santos", r)
                .andExpect(jsonPath("$[0].atendimentos.length()").value(1))
                .andExpect(jsonPath("$[0].atendimentos[0].id").value(emSantos.getId()));
        fazGet("/motoristas?nome=ana&horarioAula=07:30", r)
                .andExpect(jsonPath("$[0].atendimentos.length()").value(1))
                .andExpect(jsonPath("$[0].atendimentos[0].id").value(emCampinas.getId()));
        fazGet("/motoristas?nome=ana&horarioAula=07:31", r).andExpect(jsonPath("$.length()").value(0));
        fazGet("/motoristas?nome=ana&cidade=santos&horarioAula=07:00", r).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void pesquisaPeloNomeSoTrazValidadoEExigeAluno() throws Exception {
        Escola e = fabrica.escola();
        Motorista pendente = fabrica.motorista("Ana Pendente", StatusMotorista.PENDENTE);
        fabrica.veiculo(pendente, 5);
        fabrica.atendimento(pendente, e, AULA, SAIDA);

        fazGet("/motoristas?nome=ana", responsavelComAluno(e)).andExpect(jsonPath("$.length()").value(0));
        fazGet("/motoristas?nome=ana", fabrica.responsavel()).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ALUNO_OBRIGATORIO"));
    }

    @Test
    void pesquisaPeloNomeNaoFazConsultaPorMotorista() throws Exception {
        Escola e = fabrica.escola();
        Motorista primeiro = fabrica.motorista("Ana 0", StatusMotorista.VALIDADO);
        fabrica.veiculo(primeiro, 5);
        fabrica.atendimento(primeiro, e, AULA, SAIDA);
        Responsavel r = responsavelComAluno(e);
        long comUm = consultasDaBusca("/motoristas?nome=ana", r);

        for (int i = 1; i <= 5; i++) {
            Motorista m = fabrica.motorista("Ana " + i, StatusMotorista.VALIDADO);
            fabrica.veiculo(m, 5);
            fabrica.atendimento(m, fabrica.escola(), AULA, SAIDA);
        }
        fazGet("/motoristas?nome=ana", r).andExpect(jsonPath("$.length()").value(6));
        assertThat(consultasDaBusca("/motoristas?nome=ana", r)).isEqualTo(comUm);
    }

    // ---------------------------------------------------------------- perfil

    @Test
    void perfilAbreComAtendimentoLotadoEMostraALotacaoSoEmNumeros() throws Exception {
        Escola e = fabrica.escola("Colégio Girassol");
        Motorista m = fabrica.motorista("Ana Motorista", StatusMotorista.VALIDADO);
        fabrica.veiculo(m, 1);
        Atendimento lotado = fabrica.atendimento(m, e, AULA, SAIDA);
        Atendimento livre = fabrica.atendimento(m, e, LocalTime.of(13, 0), LocalTime.of(12, 0));
        Responsavel cliente = fabrica.responsavel("Cliente Sigiloso");
        Aluno alunoDoCliente = fabrica.aluno(cliente, e);
        fabrica.contrato(cliente, alunoDoCliente, lotado, StatusContrato.ATIVO);

        fazGet("/motoristas/" + m.getId(), responsavelComAluno(e)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Motorista"))
                .andExpect(jsonPath("$.veiculo.capacidade").value(1))
                .andExpect(jsonPath("$.atendimentos.length()").value(2))
                .andExpect(jsonPath("$.atendimentos[0].id").value(lotado.getId()))
                .andExpect(jsonPath("$.atendimentos[0].contratosAtivos").value(1))
                .andExpect(jsonPath("$.atendimentos[0].capacidade").value(1))
                .andExpect(jsonPath("$.atendimentos[0].vagasRestantes").value(0))
                .andExpect(jsonPath("$.atendimentos[1].id").value(livre.getId()))
                .andExpect(jsonPath("$.atendimentos[1].contratosAtivos").value(0))
                .andExpect(jsonPath("$.atendimentos[1].vagasRestantes").value(1))
                .andExpect(content().string(not(containsString("Cliente Sigiloso"))))
                .andExpect(content().string(not(containsString(alunoDoCliente.getNome()))));
    }

    @Test
    void perfilSemVeiculoTemCapacidadeZero() throws Exception {
        Escola e = fabrica.escola();
        Motorista m = fabrica.motorista();
        fabrica.atendimento(m, e, AULA, SAIDA);

        fazGet("/motoristas/" + m.getId(), responsavelComAluno(e)).andExpect(status().isOk())
                .andExpect(jsonPath("$.veiculo").isEmpty())
                .andExpect(jsonPath("$.atendimentos[0].capacidade").value(0))
                .andExpect(jsonPath("$.atendimentos[0].vagasRestantes").value(0));
    }

    @Test
    void totalClientesContaResponsaveisDistintosComContratoAtivoOuEncerrado() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 10);
        Motorista m = a.getMotorista();

        Responsavel familiaDeDoisFilhos = fabrica.responsavel();
        fabrica.contrato(familiaDeDoisFilhos, fabrica.aluno(familiaDeDoisFilhos, e), a, StatusContrato.ATIVO);
        fabrica.contrato(familiaDeDoisFilhos, fabrica.aluno(familiaDeDoisFilhos, e), a, StatusContrato.ENCERRADO);
        Responsavel antigo = fabrica.responsavel();
        fabrica.contrato(antigo, fabrica.aluno(antigo, e), a, StatusContrato.ENCERRADO);
        Responsavel soProposta = fabrica.responsavel();
        fabrica.contrato(soProposta, fabrica.aluno(soProposta, e), a, StatusContrato.PENDENTE);
        Responsavel recusado = fabrica.responsavel();
        fabrica.contrato(recusado, fabrica.aluno(recusado, e), a, StatusContrato.RECUSADO);
        // cliente de outro motorista não entra na conta
        Responsavel deOutro = fabrica.responsavel();
        fabrica.contrato(deOutro, fabrica.aluno(deOutro, e), atendimentoCom(e, 10), StatusContrato.ATIVO);

        fazGet("/motoristas/" + m.getId(), familiaDeDoisFilhos)
                .andExpect(jsonPath("$.totalClientes").value(2));
    }

    @Test
    void perfilDeMotoristaInexistenteOuNaoValidadoDevolve404() throws Exception {
        Escola e = fabrica.escola();
        Responsavel r = responsavelComAluno(e);
        fazGet("/motoristas/999999", r).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        fazGet("/motoristas/" + fabrica.motorista(StatusMotorista.PENDENTE).getId(), r)
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilExigeAlunoEPapelDeResponsavel() throws Exception {
        Motorista m = fabrica.motorista();
        fazGet("/motoristas/" + m.getId(), fabrica.responsavel()).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ALUNO_OBRIGATORIO"));
        fazGet("/motoristas/" + m.getId(), m).andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- proposta

    @Test
    void propostaCriaContratoPendenteNoAtendimentoENotificaOMotorista() throws Exception {
        Escola e = fabrica.escola("Colégio Girassol");
        Atendimento a = atendimentoCom(e, 2);
        Responsavel r = fabrica.responsavel();
        Aluno aluno = fabrica.aluno(r, e);

        propor(r, a, aluno).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.valorMensal").value(350.00))
                .andExpect(jsonPath("$.dataInicio").value("2026-11-03"))
                .andExpect(jsonPath("$.aluno.id").value(aluno.getId()))
                .andExpect(jsonPath("$.responsavel.id").value(r.getId()))
                .andExpect(jsonPath("$.motorista.id").value(a.getMotorista().getId()))
                .andExpect(jsonPath("$.escola.nome").value("Colégio Girassol"))
                .andExpect(jsonPath("$.atendimento.id").value(a.getId()))
                .andExpect(jsonPath("$.atendimento.horarioInicioAula").value("07:00"))
                .andExpect(jsonPath("$.cancelamento").isEmpty());

        assertThat(notificacoesDe(a.getMotorista(), TipoNotificacao.PROPOSTA_RECEBIDA)).hasSize(1);
        assertThat(eventos.stream(ContratoAtivadoEvent.class).count()).isZero();
    }

    @Test
    void propostaComCamposInvalidosDevolve400() throws Exception {
        Responsavel r = fabrica.responsavel();
        fazPost("/contratos", Map.of(), r).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.atendimentoId").exists())
                .andExpect(jsonPath("$.campos.alunoId").exists())
                .andExpect(jsonPath("$.campos.valorMensal").exists());
        fazPost("/contratos", Map.of("atendimentoId", 1, "alunoId", 1, "valorMensal", 0), r)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.valorMensal").exists());
    }

    @Test
    void propostaExigePapelDeResponsavel() throws Exception {
        fazPost("/contratos", Map.of("atendimentoId", 1, "alunoId", 1, "valorMensal", 100), fabrica.motorista())
                .andExpect(status().isForbidden());
    }

    @Test
    void propostaComAlunoOuAtendimentoInexistenteDevolve404() throws Exception {
        Escola e = fabrica.escola();
        Responsavel r = fabrica.responsavel();
        Aluno aluno = fabrica.aluno(r, e);
        fazPost("/contratos", Map.of("atendimentoId", 999999, "alunoId", aluno.getId(), "valorMensal", 100), r)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        fazPost("/contratos", Map.of("atendimentoId", atendimentoCom(e, 1).getId(), "alunoId", 999999, "valorMensal", 100), r)
                .andExpect(status().isNotFound());
    }

    @Test
    void propostaComAlunoDeOutroResponsavelDevolve403() throws Exception {
        Escola e = fabrica.escola();
        Aluno deOutro = fabrica.aluno(fabrica.responsavel(), e);

        propor(fabrica.responsavel(), atendimentoCom(e, 2), deOutro).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void propostaComAlunoDeOutraEscolaDevolve422() throws Exception {
        Responsavel r = fabrica.responsavel();
        Aluno aluno = fabrica.aluno(r, fabrica.escola());

        propor(r, atendimentoCom(fabrica.escola(), 2), aluno).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ALUNO_DE_OUTRA_ESCOLA"));
    }

    @Test
    void propostaParaMotoristaNaoValidadoDevolve422() throws Exception {
        Escola e = fabrica.escola();
        Motorista m = fabrica.motorista(StatusMotorista.PENDENTE);
        fabrica.veiculo(m, 2);
        Responsavel r = fabrica.responsavel();

        propor(r, fabrica.atendimento(m, e, AULA, SAIDA), fabrica.aluno(r, e))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("MOTORISTA_NAO_VALIDADO"));
    }

    @Test
    void propostaSemVagaNoAtendimentoDevolve409() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 1);
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.ATIVO);

        propor(r, a, fabrica.aluno(r, e)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SEM_VAGAS"));
    }

    @Test
    void vagaContaPorAtendimentoNaProposta() throws Exception {
        Escola e = fabrica.escola();
        Motorista m = fabrica.motorista();
        fabrica.veiculo(m, 1);
        Atendimento lotado = fabrica.atendimento(m, e, AULA, SAIDA);
        Atendimento livre = fabrica.atendimento(m, e, LocalTime.of(13, 0), LocalTime.of(12, 0));
        Responsavel r = fabrica.responsavel();
        fabrica.contrato(r, fabrica.aluno(r, e), lotado, StatusContrato.ATIVO);

        propor(r, livre, fabrica.aluno(r, e)).andExpect(status().isCreated());
    }

    @Test
    void motoristaSemVeiculoNaoRecebeProposta() throws Exception {
        Escola e = fabrica.escola();
        Responsavel r = fabrica.responsavel();

        propor(r, fabrica.atendimento(fabrica.motorista(), e, AULA, SAIDA), fabrica.aluno(r, e))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SEM_VAGAS"));
    }

    @Test
    void propostaPendenteIgualParaOMesmoMotoristaDevolve409MesmoEmOutroAtendimento() throws Exception {
        Escola e = fabrica.escola();
        Motorista m = fabrica.motorista();
        fabrica.veiculo(m, 5);
        Atendimento manha = fabrica.atendimento(m, e, AULA, SAIDA);
        Atendimento tarde = fabrica.atendimento(m, e, LocalTime.of(13, 0), LocalTime.of(12, 0));
        Responsavel r = fabrica.responsavel();
        Aluno aluno = fabrica.aluno(r, e);

        propor(r, manha, aluno).andExpect(status().isCreated());
        propor(r, manha, aluno).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROPOSTA_DUPLICADA"));
        propor(r, tarde, aluno).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROPOSTA_DUPLICADA"));
    }

    @Test
    void alunoComContratoAtivoNaoRecebeNovaProposta() throws Exception {
        Escola e = fabrica.escola();
        Responsavel r = fabrica.responsavel();
        Aluno aluno = fabrica.aluno(r, e);
        fabrica.contrato(r, aluno, atendimentoCom(e, 5), StatusContrato.ATIVO);

        propor(r, atendimentoCom(e, 5), aluno).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ALUNO_JA_TEM_CONTRATO"));
    }

    // ---------------------------------------------------------------- aceite

    @Test
    void aceiteAtivaNotificaEPublicaOEvento() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 1);
        Responsavel r = fabrica.responsavel();
        Contrato c = pendente(r, fabrica.aluno(r, e), a);
        relogio.definirData(LocalDate.of(2026, 11, 10));

        aceitar(c, a.getMotorista()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.dataInicio").value("2026-11-10"));

        assertThat(statusDe(c)).isEqualTo(StatusContrato.ATIVO);
        assertThat(notificacoesDe(r, TipoNotificacao.PROPOSTA_ACEITA)).hasSize(1);
        assertThat(eventos.stream(ContratoAtivadoEvent.class).map(ContratoAtivadoEvent::contratoId))
                .containsExactly(c.getId());
    }

    @Test
    void aceiteDeContratoNaoPendenteDevolve409() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Responsavel r = fabrica.responsavel();
        Contrato c = fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.ATIVO);

        aceitar(c, a.getMotorista()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONTRATO_NAO_PENDENTE"));
        assertThat(eventos.stream(ContratoAtivadoEvent.class).count()).isZero();
    }

    @Test
    void aceiteRechecaAVagaDoAtendimento() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 1);
        Responsavel r = fabrica.responsavel();
        Contrato primeiro = pendente(r, fabrica.aluno(r, e), a);
        Contrato segundo = pendente(r, fabrica.aluno(r, e), a);

        aceitar(primeiro, a.getMotorista()).andExpect(status().isOk());
        aceitar(segundo, a.getMotorista()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SEM_VAGAS"));

        assertThat(statusDe(segundo)).isEqualTo(StatusContrato.PENDENTE);
        assertThat(eventos.stream(ContratoAtivadoEvent.class).count()).isEqualTo(1);
    }

    @Test
    void vagaContaPorAtendimentoNoAceite() throws Exception {
        Escola e = fabrica.escola();
        Motorista m = fabrica.motorista();
        fabrica.veiculo(m, 1);
        Atendimento manha = fabrica.atendimento(m, e, AULA, SAIDA);
        Atendimento tarde = fabrica.atendimento(m, e, LocalTime.of(13, 0), LocalTime.of(12, 0));
        Responsavel r = fabrica.responsavel();

        aceitar(pendente(r, fabrica.aluno(r, e), manha), m).andExpect(status().isOk());
        aceitar(pendente(r, fabrica.aluno(r, e), tarde), m).andExpect(status().isOk());
    }

    @Test
    void alunoNaoFicaComDoisContratosAtivosAoAceitarAsegundaProposta() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a1 = atendimentoCom(e, 5);
        Atendimento a2 = atendimentoCom(e, 5);
        Responsavel r = fabrica.responsavel();
        Aluno aluno = fabrica.aluno(r, e);
        Contrato c1 = pendente(r, aluno, a1);
        Contrato c2 = pendente(r, aluno, a2);

        aceitar(c1, a1.getMotorista()).andExpect(status().isOk());
        aceitar(c2, a2.getMotorista()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ALUNO_JA_TEM_CONTRATO"));
    }

    @Test
    void soOMotoristaDonoAceitaOuRecusa() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Responsavel r = fabrica.responsavel();
        Contrato c = pendente(r, fabrica.aluno(r, e), a);
        Motorista outro = fabrica.motorista();

        aceitar(c, outro).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        recusar(c, outro).andExpect(status().isForbidden());
        // responsável não tem o papel da rota
        fazPostSemCorpo("/contratos/" + c.getId() + "/aceitar", r).andExpect(status().isForbidden());
        assertThat(statusDe(c)).isEqualTo(StatusContrato.PENDENTE);
    }

    @Test
    void aceitarOuRecusarContratoInexistenteDevolve404() throws Exception {
        Motorista m = fabrica.motorista();
        fazPostSemCorpo("/contratos/999999/aceitar", m).andExpect(status().isNotFound());
        fazPostSemCorpo("/contratos/999999/recusar", m).andExpect(status().isNotFound());
    }

    private record Requisicao(String url, Motorista motorista) {
        static Requisicao aceite(Long contratoId, Motorista m) {
            return new Requisicao("/contratos/" + contratoId + "/aceitar", m);
        }

        static Requisicao recusa(Long contratoId, Motorista m) {
            return new Requisicao("/contratos/" + contratoId + "/recusar", m);
        }
    }

    /** Dispara todas as requisições juntas e devolve os status HTTP. */
    private List<Integer> simultaneos(List<Requisicao> requisicoes) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requisicoes.size());
        List<Integer> statusHttp = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> tarefas = new ArrayList<>();
            for (Requisicao req : requisicoes) {
                tarefas.add(pool.submit(() -> {
                    largada.await();
                    statusHttp.add(fazPostSemCorpo(req.url(), req.motorista())
                            .andReturn().getResponse().getStatus());
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

    @Test
    void aceitesSimultaneosComUmaVagaSoUmPassa() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 1);
        Responsavel r = fabrica.responsavel();
        List<Requisicao> aceites = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            aceites.add(Requisicao.aceite(pendente(r, fabrica.aluno(r, e), a).getId(), a.getMotorista()));
        }

        List<Integer> statusHttp = simultaneos(aceites);

        assertThat(statusHttp).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statusHttp).filteredOn(s -> s == 409).hasSize(3);
        assertThat(contratoRepository.findAll()).filteredOn(c -> c.getStatus() == StatusContrato.ATIVO).hasSize(1);
    }

    @Test
    void aceitesSimultaneosDoMesmoContratoSoUmPassa() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Responsavel r = fabrica.responsavel();
        Requisicao aceite = Requisicao.aceite(pendente(r, fabrica.aluno(r, e), a).getId(), a.getMotorista());

        List<Integer> statusHttp = simultaneos(List.of(aceite, aceite, aceite, aceite));

        assertThat(statusHttp).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statusHttp).filteredOn(s -> s == 409).hasSize(3);
    }

    @Test
    void aceiteERecusaSimultaneosDoMesmoContratoSoUmPassa() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Motorista m = a.getMotorista();
        Responsavel r = fabrica.responsavel();
        Long id = pendente(r, fabrica.aluno(r, e), a).getId();

        List<Integer> statusHttp = simultaneos(List.of(Requisicao.aceite(id, m), Requisicao.recusa(id, m),
                Requisicao.aceite(id, m), Requisicao.recusa(id, m)));

        assertThat(statusHttp).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statusHttp).filteredOn(s -> s == 409).hasSize(3);
    }

    @Test
    void aceitesSimultaneosDoMesmoAlunoComMotoristasDiferentesSoUmPassa() throws Exception {
        Escola e = fabrica.escola();
        Responsavel r = fabrica.responsavel();
        Aluno aluno = fabrica.aluno(r, e);
        List<Requisicao> aceites = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Atendimento a = atendimentoCom(e, 5);
            aceites.add(Requisicao.aceite(pendente(r, aluno, a).getId(), a.getMotorista()));
        }

        List<Integer> statusHttp = simultaneos(aceites);

        assertThat(statusHttp).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statusHttp).filteredOn(s -> s == 409).hasSize(3);
    }

    // ---------------------------------------------------------------- recusa

    @Test
    void recusaMudaOStatusENotificaOResponsavel() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Responsavel r = fabrica.responsavel();
        Contrato c = pendente(r, fabrica.aluno(r, e), a);

        recusar(c, a.getMotorista()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADO"));

        assertThat(statusDe(c)).isEqualTo(StatusContrato.RECUSADO);
        assertThat(notificacoesDe(r, TipoNotificacao.PROPOSTA_RECUSADA)).hasSize(1);
        assertThat(eventos.stream(ContratoAtivadoEvent.class).count()).isZero();
    }

    @Test
    void recusaDeContratoNaoPendenteDevolve409() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Responsavel r = fabrica.responsavel();
        Contrato c = fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.RECUSADO);

        recusar(c, a.getMotorista()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONTRATO_NAO_PENDENTE"));
    }

    // ---------------------------------------------------------------- listagem e consulta

    @Test
    void listagemTrazOsContratosDoUsuarioMaisRecentesPrimeiroEFiltraPorStatus() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Responsavel r = fabrica.responsavel();
        Contrato antigo = fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.ATIVO);
        Contrato novo = pendente(r, fabrica.aluno(r, e), a);
        Responsavel outro = fabrica.responsavel();
        Contrato deOutro = pendente(outro, fabrica.aluno(outro, e), atendimentoCom(e, 5));

        fazGet("/contratos", r).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(novo.getId()))
                .andExpect(jsonPath("$[1].id").value(antigo.getId()));
        fazGet("/contratos?status=ATIVO", r)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(antigo.getId()));
        fazGet("/contratos", a.getMotorista())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        antigo.getId().intValue(), novo.getId().intValue())));
        fazGet("/contratos", outro).andExpect(jsonPath("$[0].id").value(deOutro.getId()));
    }

    @Test
    void listagemComStatusInvalidoDevolve400() throws Exception {
        fazGet("/contratos?status=XPTO", fabrica.responsavel()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void consultaSoParaAsPartes() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Responsavel r = fabrica.responsavel();
        Contrato c = pendente(r, fabrica.aluno(r, e), a);
        String url = "/contratos/" + c.getId();

        fazGet(url, r).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(c.getId()));
        fazGet(url, a.getMotorista()).andExpect(status().isOk());
        fazGet(url, fabrica.responsavel()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        fazGet(url, fabrica.motorista()).andExpect(status().isForbidden());
        fazGet("/contratos/999999", r).andExpect(status().isNotFound());
    }

    @Test
    void contratosExigemLogin() throws Exception {
        fazGet("/contratos").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    // ---------------------------------------------------------------- alterar atendimentos com contratos

    private Map<String, Object> listaDeAtendimentos(Object... itens) {
        return Map.of("atendimentos", List.of(itens));
    }

    private Map<String, Object> itemAtendimento(Escola e, String aula, String saida) {
        return Map.of("escolaId", e.getId(), "horarioInicioAula", aula, "horarioSaidaMotorista", saida);
    }

    @Test
    void atendimentoQueContinuaNaListaMantemOIdEOContrato() throws Exception {
        Escola e = fabrica.escola();
        Atendimento a = atendimentoCom(e, 5);
        Motorista m = a.getMotorista();
        Responsavel r = fabrica.responsavel();
        Contrato c = fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.ATIVO);

        fazPut("/motorista/escolas", listaDeAtendimentos(
                itemAtendimento(e, "07:00", "06:00"), itemAtendimento(e, "13:00", "12:00")), m)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(a.getId()));
        fazGet("/contratos/" + c.getId(), r).andExpect(jsonPath("$.atendimento.id").value(a.getId()));
    }

    @Test
    void naoRemoveAtendimentoComPropostaPendenteOuContratoAtivo() throws Exception {
        for (StatusContrato status : List.of(StatusContrato.PENDENTE, StatusContrato.ATIVO)) {
            Escola e = fabrica.escola();
            Atendimento a = atendimentoCom(e, 5);
            Responsavel r = fabrica.responsavel();
            fabrica.contrato(r, fabrica.aluno(r, e), a, status);

            fazPut("/motorista/escolas", listaDeAtendimentos(), a.getMotorista())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ATENDIMENTO_COM_CONTRATO"));
            fazGet("/motorista/escolas", a.getMotorista()).andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Test
    void removerAtendimentoComContratoTerminadoGuardaOHistoricoSemOTurno() throws Exception {
        Escola e = fabrica.escola("Colégio Girassol");
        Atendimento a = atendimentoCom(e, 5);
        Motorista m = a.getMotorista();
        Responsavel r = fabrica.responsavel();
        Contrato encerrado = fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.ENCERRADO);
        Contrato recusado = fabrica.contrato(r, fabrica.aluno(r, e), a, StatusContrato.RECUSADO);

        fazPut("/motorista/escolas", listaDeAtendimentos(), m).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        fazGet("/contratos/" + encerrado.getId(), r).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADO"))
                .andExpect(jsonPath("$.atendimento").isEmpty())
                .andExpect(jsonPath("$.escola.nome").value("Colégio Girassol"));
        fazGet("/contratos/" + recusado.getId(), r).andExpect(status().isOk());
        // o motorista continua com o cliente que já teve, mesmo sem o turno
        fazGet("/motoristas/" + m.getId(), r).andExpect(jsonPath("$.totalClientes").value(1));
    }
}
