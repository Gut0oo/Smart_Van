package br.com.smartvan;

import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.repository.FaltaRepository;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FaltaIntegracaoTest extends TesteIntegracaoBase {

    @Autowired FaltaRepository faltas;

    Responsavel resp;
    Aluno aluno;
    String url;
    String amanha;

    @BeforeEach
    void cenario() {
        resp = fabrica.responsavel();
        aluno = fabrica.aluno(resp, fabrica.escola());
        url = "/alunos/" + aluno.getId() + "/faltas";
        amanha = relogio.hoje().plusDays(1).toString();
    }

    @Test
    void responsavelRegistraFaltaFutura() throws Exception {
        fazPost(url, Map.of("data", amanha, "motivo", "consulta"), resp)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.data").value(amanha))
                .andExpect(jsonPath("$.motivo").value("consulta"));
    }

    @Test
    void faltaHojeEPermitidaEMotivoEOpcional() throws Exception {
        fazPost(url, Map.of("data", relogio.hoje().toString()), resp)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motivo").value(nullValue()));
    }

    @Test
    void dataPassadaDa422() throws Exception {
        fazPost(url, Map.of("data", relogio.hoje().minusDays(1).toString()), resp)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("DATA_PASSADA"));
    }

    @Test
    void duplicadaDa409() throws Exception {
        fazPost(url, Map.of("data", amanha), resp).andExpect(status().isCreated());

        fazPost(url, Map.of("data", amanha), resp)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("FALTA_JA_REGISTRADA"));
    }

    @Test
    void duplicadasSimultaneasGravamUmaSo() throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Integer> statusHttp = Collections.synchronizedList(new ArrayList<>());
        try {
            List<Future<?>> tarefas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tarefas.add(pool.submit(() -> {
                    largada.await();
                    statusHttp.add(fazPost(url, Map.of("data", amanha), resp).andReturn().getResponse().getStatus());
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

        assertThat(statusHttp).containsExactlyInAnyOrder(201, 409);
        assertThat(faltas.count()).isEqualTo(1);
    }

    @Test
    void semDataDa400() throws Exception {
        fazPost(url, Map.of("motivo", "x"), resp)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void alunoDeOutroResponsavelDa403() throws Exception {
        Responsavel outro = fabrica.responsavel();

        fazPost(url, Map.of("data", amanha), outro)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        fazGet(url, outro).andExpect(status().isForbidden());
        fazDelete(url + "/" + amanha, outro).andExpect(status().isForbidden());
    }

    @Test
    void alunoInexistenteDa404() throws Exception {
        fazPost("/alunos/999999/faltas", Map.of("data", amanha), resp)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        fazGet("/alunos/999999/faltas", resp).andExpect(status().isNotFound());
        fazDelete("/alunos/999999/faltas/" + amanha, resp).andExpect(status().isNotFound());
    }

    @Test
    void motoristaNaoRegistraFalta() throws Exception {
        fazPost(url, Map.of("data", amanha), fabrica.motorista())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void listaFaltasDoMesEmOrdemDeData() throws Exception {
        //hoje = 2026-11-03
        fabrica.falta(aluno, LocalDate.of(2026, 11, 20));
        fabrica.falta(aluno, LocalDate.of(2026, 11, 5));
        fabrica.falta(aluno, LocalDate.of(2026, 12, 1));

        fazGet(url + "?mes=2026-11", resp)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].data").value("2026-11-05"))
                .andExpect(jsonPath("$[1].data").value("2026-11-20"));
        fazGet(url + "?mes=2026-12", resp).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void semMesListaDeHojeEmDiante() throws Exception {
        fabrica.falta(aluno, relogio.hoje().minusDays(1));
        fabrica.falta(aluno, relogio.hoje());
        fabrica.falta(aluno, relogio.hoje().plusDays(40));

        fazGet(url, resp)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].data").value(relogio.hoje().toString()));
    }

    @Test
    void mesMalFormadoDa400() throws Exception {
        fazGet(url + "?mes=novembro", resp)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void removeFaltaESemFaltaDa404() throws Exception {
        fazPost(url, Map.of("data", amanha), resp).andExpect(status().isCreated());

        fazDelete(url + "/" + amanha, resp).andExpect(status().isNoContent());

        assertThat(faltas.count()).isZero();
        fazDelete(url + "/" + amanha, resp)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
