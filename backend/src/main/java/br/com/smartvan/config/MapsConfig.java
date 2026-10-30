package br.com.smartvan.config;

import br.com.smartvan.maps.MapsIndisponivel;
import br.com.smartvan.maps.MapsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** A etapa de rota registra a implementação do Google quando houver chave; senão vale {@link MapsIndisponivel}. */
@Configuration
public class MapsConfig {

    @Bean
    @ConditionalOnMissingBean(MapsProvider.class)
    public MapsProvider mapsIndisponivel() {
        return new MapsIndisponivel();
    }
}
