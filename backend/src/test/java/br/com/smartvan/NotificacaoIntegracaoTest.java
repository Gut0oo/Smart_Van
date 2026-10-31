package br.com.smartvan;

import br.com.smartvan.dto.NotificacaoResponseDTO;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.service.NotificacaoService;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** NotificacaoService e rotas de /notificacoes (seção 6 do contrato). */
class NotificacaoIntegracaoTest extends TesteIntegracaoBase {

    @Autowired NotificacaoService service;

    @Test
    void notificarCriaNaoLidaComDataDoRelogioEListarTrazMaisRecentesPrimeiro() {
        Responsavel r = fabrica.responsavel();
        service.notificar(r.getId(), TipoNotificacao.ROTA_GERADA, "Rota", "Sua rota saiu", 7L);
        relogio.avancarSegundos(60);
        service.notificar(r.getId(), TipoNotificacao.MENSALIDADE_GERADA, "Mensalidade", "Gerada", null);

        List<NotificacaoResponseDTO> lista = service.listar(r.getId(), null);

        assertThat(lista).extracting(NotificacaoResponseDTO::tipo)
                .containsExactly(TipoNotificacao.MENSALIDADE_GERADA, TipoNotificacao.ROTA_GERADA);
        NotificacaoResponseDTO rota = lista.get(1);
        assertThat(rota.id()).isNotNull();
        assertThat(rota.titulo()).isEqualTo("Rota");
        assertThat(rota.mensagem()).isEqualTo("Sua rota saiu");
        assertThat(rota.referenciaId()).isEqualTo(7L);
        assertThat(rota.lida()).isFalse();
        assertThat(rota.dataCriacao()).isEqualTo(relogio.instant().minusSeconds(60));
        assertThat(lista.get(0).referenciaId()).isNull();
    }

    @Test
    void listarSoTrazAsDoProprioUsuario() {
        Responsavel r = fabrica.responsavel();
        Responsavel outro = fabrica.responsavel();
        service.notificar(outro.getId(), TipoNotificacao.ROTA_GERADA, "Rota", "Do outro", null);

        assertThat(service.listar(r.getId(), null)).isEmpty();
        assertThat(service.listar(outro.getId(), null)).hasSize(1);
    }

    @Test
    void marcarComoLidaFiltraListagemEContagem() {
        Responsavel r = fabrica.responsavel();
        service.notificar(r.getId(), TipoNotificacao.ROTA_GERADA, "Rota", "m", null);
        service.notificar(r.getId(), TipoNotificacao.MENSALIDADE_PAGA, "Paga", "m", null);
        assertThat(service.contarNaoLidas(r.getId())).isEqualTo(2);

        Long id = notificacoesDe(r, TipoNotificacao.ROTA_GERADA).getFirst().getId();
        service.marcarComoLida(r.getId(), id);

        assertThat(service.contarNaoLidas(r.getId())).isEqualTo(1);
        assertThat(service.listar(r.getId(), true)).extracting(NotificacaoResponseDTO::id).containsExactly(id);
        assertThat(service.listar(r.getId(), false)).extracting(NotificacaoResponseDTO::tipo)
                .containsExactly(TipoNotificacao.MENSALIDADE_PAGA);
    }

    @Test
    void marcarComoLidaDeOutroUsuarioDaSemPermissao() {
        Responsavel dono = fabrica.responsavel();
        Responsavel outro = fabrica.responsavel();
        service.notificar(dono.getId(), TipoNotificacao.ROTA_GERADA, "Rota", "m", null);
        Long id = notificacoesDe(dono).getFirst().getId();

        assertThatThrownBy(() -> service.marcarComoLida(outro.getId(), id))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SEM_PERMISSAO"));
        assertThat(service.contarNaoLidas(dono.getId())).isEqualTo(1);
    }

    @Test
    void marcarComoLidaInexistenteDaNaoEncontrado() {
        Responsavel r = fabrica.responsavel();

        assertThatThrownBy(() -> service.marcarComoLida(r.getId(), 999_999L))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("NAO_ENCONTRADO"));
    }

    // ---------------------------------------------------------------- rotas

    @Test
    void rotasSemTokenDao401() throws Exception {
        fazGet("/notificacoes").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        fazGet("/notificacoes/nao-lidas/contagem").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        fazPost("/notificacoes/1/lida").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void getNotificacoesListaSoAsDoLogadoMaisRecentesPrimeiroComOsCamposDoContrato() throws Exception {
        Responsavel r = fabrica.responsavel();
        Responsavel outro = fabrica.responsavel();
        service.notificar(r.getId(), TipoNotificacao.ROTA_GERADA, "Rota", "Sua rota saiu", 7L);
        relogio.avancarSegundos(60);
        service.notificar(r.getId(), TipoNotificacao.MENSALIDADE_GERADA, "Mensalidade", "Gerada", null);
        service.notificar(outro.getId(), TipoNotificacao.ROTA_GERADA, "Rota", "Do outro", null);

        fazGet("/notificacoes", r)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("MENSALIDADE_GERADA"))
                .andExpect(jsonPath("$[1].id").isNumber())
                .andExpect(jsonPath("$[1].tipo").value("ROTA_GERADA"))
                .andExpect(jsonPath("$[1].titulo").value("Rota"))
                .andExpect(jsonPath("$[1].mensagem").value("Sua rota saiu"))
                .andExpect(jsonPath("$[1].referenciaId").value(7))
                .andExpect(jsonPath("$[1].lida").value(false))
                .andExpect(jsonPath("$[1].dataCriacao").value("2026-11-03T12:00:00Z"));
    }

    @Test
    void getNotificacoesFiltraPorLidasEContagemSoContaNaoLidas() throws Exception {
        Responsavel r = fabrica.responsavel();
        service.notificar(r.getId(), TipoNotificacao.ROTA_GERADA, "Rota", "m", null);
        service.notificar(r.getId(), TipoNotificacao.MENSALIDADE_PAGA, "Paga", "m", null);
        fazGet("/notificacoes/nao-lidas/contagem", r)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));

        Long id = notificacoesDe(r, TipoNotificacao.ROTA_GERADA).getFirst().getId();
        fazPostSemCorpo("/notificacoes/" + id + "/lida", r).andExpect(status().isNoContent());

        fazGet("/notificacoes/nao-lidas/contagem", r).andExpect(jsonPath("$.total").value(1));
        fazGet("/notificacoes?lidas=true", r)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        fazGet("/notificacoes?lidas=false", r)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("MENSALIDADE_PAGA"));
    }

    @Test
    void marcarComoLidaDeOutroUsuarioPelaRotaDa403() throws Exception {
        Responsavel dono = fabrica.responsavel();
        Responsavel outro = fabrica.responsavel();
        service.notificar(dono.getId(), TipoNotificacao.ROTA_GERADA, "Rota", "m", null);
        Long id = notificacoesDe(dono).getFirst().getId();

        fazPostSemCorpo("/notificacoes/" + id + "/lida", outro)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        assertThat(service.contarNaoLidas(dono.getId())).isEqualTo(1);
    }

    @Test
    void marcarComoLidaInexistentePelaRotaDa404() throws Exception {
        fazPostSemCorpo("/notificacoes/999999/lida", fabrica.responsavel())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
