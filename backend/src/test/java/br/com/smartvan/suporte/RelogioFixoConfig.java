package br.com.smartvan.suporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Clock @Primary dos testes: 2026-11-03 09:00 (São Paulo), avançável via {@link RelogioMutavel}. */
@TestConfiguration
public class RelogioFixoConfig {

    @Bean
    @Primary
    public RelogioMutavel relogioDeTeste() {
        return new RelogioMutavel();
    }
}
