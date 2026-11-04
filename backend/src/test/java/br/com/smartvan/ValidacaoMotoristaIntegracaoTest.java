package br.com.smartvan;

import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Administrador;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Validacao;
import br.com.smartvan.repository.MotoristaRepository;
import br.com.smartvan.repository.ValidacaoRepository;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidacaoMotoristaIntegracaoTest extends TesteIntegracaoBase {

    @Autowired ValidacaoRepository validacoes;
    @Autowired MotoristaRepository motoristas;

    Administrador adm;
    Motorista pendente;

    @BeforeEach
    void cenario() {
        adm = fabrica.administrador();
        pendente = fabrica.motorista(StatusMotorista.PENDENTE);
    }

    private ResultActions decidir(Motorista m, Map<String, Object> corpo) throws Exception {
        return fazPost("/admin/motoristas/" + m.getId() + "/validacao", corpo, adm);
    }

    private StatusMotorista statusDe(Motorista m) {
        return motoristas.findById(m.getId()).orElseThrow().getStatusValidacao();
    }

    @Test
    void listaSoOsMotoristasSemDecisao() throws Exception {
        Motorista emAnalise = fabrica.motorista(StatusMotorista.EM_ANALISE);
        fabrica.motorista(StatusMotorista.VALIDADO);
        fabrica.motorista(StatusMotorista.REJEITADO);

        fazGet("/admin/motoristas/pendentes", adm).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(pendente.getId()))
                .andExpect(jsonPath("$[0].statusValidacao").value("PENDENTE"))
                .andExpect(jsonPath("$[1].id").value(emAnalise.getId()));
    }

    @Test
    void aprovarGravaValidacaoAuditoriaENotifica() throws Exception {
        decidir(pendente, Map.of("decisao", "VALIDADO")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pendente.getId()))
                .andExpect(jsonPath("$.statusValidacao").value("VALIDADO"));

        assertThat(statusDe(pendente)).isEqualTo(StatusMotorista.VALIDADO);
        List<Validacao> gravadas = validacoes.findAll();
        assertThat(gravadas).singleElement().satisfies(v -> {
            assertThat(v.getDecisao()).isEqualTo(StatusMotorista.VALIDADO);
            assertThat(v.getParecer()).isNull();
        });
        assertThat(auditorias("Motorista", pendente.getId())).singleElement()
                .satisfies(a -> assertThat(a.getAcao()).isEqualTo("VALIDACAO_MOTORISTA"));
        assertThat(notificacoesDe(pendente, TipoNotificacao.PROPOSTA_ACEITA)).singleElement()
                .satisfies(n -> assertThat(n.getTitulo()).isEqualTo("Cadastro aprovado"));
    }

    @Test
    void rejeitarGuardaOParecerENotifica() throws Exception {
        decidir(pendente, Map.of("decisao", "REJEITADO", "parecer", " CNH vencida ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.statusValidacao").value("REJEITADO"));

        assertThat(validacoes.findAll()).singleElement()
                .satisfies(v -> assertThat(v.getParecer()).isEqualTo("CNH vencida"));
        assertThat(notificacoesDe(pendente, TipoNotificacao.PROPOSTA_RECUSADA)).singleElement()
                .satisfies(n -> assertThat(n.getMensagem()).contains("CNH vencida"));
    }

    @Test
    void rejeitarSemParecerDevolve422ENaoMudaNada() throws Exception {
        decidir(pendente, Map.of("decisao", "REJEITADO", "parecer", "  "))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("PARECER_OBRIGATORIO"));

        assertThat(statusDe(pendente)).isEqualTo(StatusMotorista.PENDENTE);
        assertThat(validacoes.findAll()).isEmpty();
    }

    @Test
    void decisaoInvalidaOuAusenteDevolve400() throws Exception {
        decidir(pendente, Map.of("decisao", "PENDENTE")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
        decidir(pendente, Map.of("decisao", "EM_ANALISE")).andExpect(status().isBadRequest());
        decidir(pendente, Map.of("decisao", "XPTO")).andExpect(status().isBadRequest());
        decidir(pendente, Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.decisao").exists());
    }

    @Test
    void motoristaJaAvaliadoDevolve409() throws Exception {
        decidir(pendente, Map.of("decisao", "VALIDADO")).andExpect(status().isOk());

        decidir(pendente, Map.of("decisao", "REJEITADO", "parecer", "Mudei de ideia"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MOTORISTA_JA_AVALIADO"));

        assertThat(statusDe(pendente)).isEqualTo(StatusMotorista.VALIDADO);
    }

    @Test
    void motoristaInexistenteOuDeOutroTipoDevolve404() throws Exception {
        fazPost("/admin/motoristas/999999/validacao", Map.of("decisao", "VALIDADO"), adm)
                .andExpect(status().isNotFound());
        fazPost("/admin/motoristas/" + fabrica.responsavel().getId() + "/validacao", Map.of("decisao", "VALIDADO"), adm)
                .andExpect(status().isNotFound());
    }

    @Test
    void rotasSaoDoAdministrador() throws Exception {
        String url = "/admin/motoristas/" + pendente.getId() + "/validacao";
        fazGet("/admin/motoristas/pendentes").andExpect(status().isUnauthorized());
        fazGet("/admin/motoristas/pendentes", fabrica.responsavel()).andExpect(status().isForbidden());
        fazPost(url, Map.of("decisao", "VALIDADO"), fabrica.responsavel()).andExpect(status().isForbidden());
        //o motorista não aprova a si mesmo
        fazPost(url, Map.of("decisao", "VALIDADO"), pendente).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));

        assertThat(statusDe(pendente)).isEqualTo(StatusMotorista.PENDENTE);
    }

    @Test
    void motoristaAprovadoPassaAAparecerNaBusca() throws Exception {
        Escola escola = fabrica.escola();
        fabrica.veiculo(pendente, 5);
        fabrica.atendimento(pendente, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        Responsavel r = fabrica.responsavel();
        fabrica.aluno(r, escola);
        String busca = "/motoristas?escolaId=" + escola.getId();

        fazGet(busca, r).andExpect(jsonPath("$.length()").value(0));
        decidir(pendente, Map.of("decisao", "VALIDADO")).andExpect(status().isOk());
        fazGet(busca, r).andExpect(jsonPath("$[0].id").value(pendente.getId()));
    }

    @Test
    void cadastroPublicoPermiteCriarAdministrador() throws Exception {
        Map<String, Object> corpo = Map.of("nome", "Adm Público", "email", "adm.publico@teste.com", "senha", "senha123",
                "cpf", "98765432100", "telefone", "11999990000", "tipo", TipoUsuario.ADMINISTRADOR.name());

        fazPost("/usuarios", corpo).andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ADMINISTRADOR"));
    }
}
