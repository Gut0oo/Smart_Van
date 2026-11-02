package br.com.smartvan.config;

import br.com.smartvan.maps.GoogleMapsProvider;
import br.com.smartvan.maps.MapsIndisponivel;
import br.com.smartvan.maps.MapsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Um único bean de mapas: Google quando smartvan.google-maps.api-key (GOOGLE_MAPS_API_KEY) está preenchida,
 * {@link MapsIndisponivel} quando não está. A escolha fica aqui para não depender da ordem de avaliação das configurações.
 */
@Configuration
public class MapsConfig {

    @Bean
    public MapsProvider mapsProvider(@Value("${smartvan.google-maps.api-key:}") String chave, Clock clock) {
        return chave.isBlank() ? new MapsIndisponivel() : new GoogleMapsProvider(chave.trim(), clock);
    }
}
