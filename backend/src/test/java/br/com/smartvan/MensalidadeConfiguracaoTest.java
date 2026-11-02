package br.com.smartvan;

import br.com.smartvan.config.AgendadorConfig;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.MensalidadeRepository;
import br.com.smartvan.service.AuditoriaService;
import br.com.smartvan.service.MensalidadeJob;
import br.com.smartvan.service.MensalidadeService;
import br.com.smartvan.service.NotificacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;

import java.time.Clock;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobe só os beans de mensalidade, com mocks em volta, para testar a configuração sem o resto da aplicação. */
class MensalidadeConfiguracaoTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withBean(MensalidadeRepository.class, () -> Mockito.mock(MensalidadeRepository.class))
            .withBean(ContratoRepository.class, () -> Mockito.mock(ContratoRepository.class))
            .withBean(NotificacaoService.class, () -> Mockito.mock(NotificacaoService.class))
            .withBean(AuditoriaService.class, () -> Mockito.mock(AuditoriaService.class))
            .withBean(Clock.class, Clock::systemUTC)
            .withUserConfiguration(MensalidadeService.class);

    @ParameterizedTest
    @ValueSource(ints = {0, 29, -1})
    void aplicacaoNaoSobeComDiaDeVencimentoForaDaFaixa(int dia) {
        contexto.withPropertyValues("smartvan.mensalidade.dia-vencimento=" + dia).run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("smartvan.mensalidade.dia-vencimento deve estar entre 1 e 28");
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 28})
    void aplicacaoSobeComDiaDeVencimentoNaFaixa(int dia) {
        contexto.withPropertyValues("smartvan.mensalidade.dia-vencimento=" + dia)
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    /** Nos testes de integração o agendador fica desligado: aqui lê o application.yml de produção e o liga. */
    @Test
    void configuracaoDeProducaoAgendaOJobNoCronDoContrato() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));
        Properties producao = yaml.getObject();

        assertThat(producao.getProperty("smartvan.mensalidade.dia-vencimento")).isEqualTo("10");

        contexto.withUserConfiguration(AgendadorConfig.class, MensalidadeJob.class)
                .withInitializer(ctx -> ctx.getEnvironment().getPropertySources()
                        .addFirst(new PropertiesPropertySource("producao", producao)))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks())
                            .extracting(tarefa -> ((CronTask) tarefa.getTask()).getExpression())
                            .containsExactly("0 0 6 * * *");
                });
    }
}
