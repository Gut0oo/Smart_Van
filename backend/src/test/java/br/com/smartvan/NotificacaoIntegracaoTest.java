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

/** NotificacaoService (seção 6 do contrato). Os testes das rotas entram na etapa 2, com o login. */
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
}
