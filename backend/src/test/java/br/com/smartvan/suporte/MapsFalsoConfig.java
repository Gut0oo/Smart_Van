package br.com.smartvan.suporte;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Registra o {@link FakeMapsProvider} como MapsProvider @Primary (testes nunca chamam o Google). */
@TestConfiguration
public class MapsFalsoConfig {

    @Bean
    @Primary
    public FakeMapsProvider mapsFalso() {
        return new FakeMapsProvider();
    }
}
