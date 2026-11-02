package br.com.smartvan.maps;

import br.com.smartvan.config.MapsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

//existe sempre um único MapsProvider: o Google com chave, o MapsIndisponivel sem chave
class MapsConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withUserConfiguration(MapsConfig.class);

    @Test
    void semAPropriedadeSoExisteOMapsIndisponivel() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(MapsProvider.class);
            assertThat(ctx.getBean(MapsProvider.class)).isInstanceOf(MapsIndisponivel.class);
        });
    }

    @Test
    void chaveVaziaOuEmBrancoSoExisteOMapsIndisponivel() {
        for (String chave : new String[] {"", "   "}) {
            runner.withPropertyValues("smartvan.google-maps.api-key=" + chave).run(ctx -> {
                assertThat(ctx).hasSingleBean(MapsProvider.class);
                assertThat(ctx.getBean(MapsProvider.class)).isInstanceOf(MapsIndisponivel.class);
            });
        }
    }

    @Test
    void comChaveSoExisteOGoogle() {
        runner.withPropertyValues("smartvan.google-maps.api-key=chave-de-teste").run(ctx -> {
            assertThat(ctx).hasSingleBean(MapsProvider.class);
            assertThat(ctx.getBean(MapsProvider.class)).isInstanceOf(GoogleMapsProvider.class);
        });
    }
}
