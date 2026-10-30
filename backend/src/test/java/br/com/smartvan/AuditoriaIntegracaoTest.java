package br.com.smartvan;

import br.com.smartvan.model.Auditoria;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.service.AuditoriaService;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaIntegracaoTest extends TesteIntegracaoBase {

    @Autowired AuditoriaService service;

    @Test
    void registrarGravaAcaoEntidadeUsuarioEDataDoRelogio() {
        Responsavel r = fabrica.responsavel();

        service.registrar(r.getId(), "BAIXA_MENSALIDADE", "Mensalidade", 8L, "pago em dinheiro");
        service.registrar(null, "OUTRA", "Mensalidade", 9L, null);

        assertThat(auditorias("Mensalidade", 8L)).singleElement().satisfies(a -> {
            assertThat(a.getId()).isNotNull();
            assertThat(a.getUsuarioId()).isEqualTo(r.getId());
            assertThat(a.getAcao()).isEqualTo("BAIXA_MENSALIDADE");
            assertThat(a.getDetalhes()).isEqualTo("pago em dinheiro");
            assertThat(a.getDataRegistro()).isEqualTo(relogio.instant());
        });
        assertThat(auditorias("Mensalidade", 9L)).singleElement()
                .extracting(Auditoria::getUsuarioId).isNull();
    }
}
