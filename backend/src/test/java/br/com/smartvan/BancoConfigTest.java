package br.com.smartvan;

import br.com.smartvan.config.BancoConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;

class BancoConfigTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(BancoConfig.class);

    @Test
    void semAVariavelAAplicacaoNaoSobeEDizOMotivo() {
        contexto.withPropertyValues("spring.datasource.password=${SENHA_QUE_NAO_EXISTE_NO_TESTE}").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).hasStackTraceContaining("Defina a variável de ambiente DB_PASSWORD");
        });
    }

    @Test
    void comASenhaDefinidaAAplicacaoSobe() {
        contexto.withPropertyValues("spring.datasource.password=qualquer").run(ctx -> assertThat(ctx).hasNotFailed());
    }

    @Test
    void configuracaoDeProducaoNaoTemSenhaPadrao() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource("src/main/resources/application.yml"));

        assertThat(yaml.getObject().getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
    }
}
