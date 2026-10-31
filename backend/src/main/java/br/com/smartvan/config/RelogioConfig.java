package br.com.smartvan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * "Hoje" no negócio é sempre em America/Sao_Paulo. Injete {@link Clock} e use LocalDate.now(clock).
 * Nos testes, o RelogioFixoConfig registra um relógio mutável @Primary.
 */
@Configuration
public class RelogioConfig {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA);
    }
}
