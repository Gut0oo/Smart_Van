package br.com.smartvan.config;

import br.com.smartvan.repository.UsuarioRepository;
import br.com.smartvan.security.InterceptorUsuarioLogado;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Configuration
public class InterceptorConfig {

    //MappedInterceptor vale para todos os handler mappings, inclusive o do Actuator; o WebMvcConfigurer não cobriria esse
    @Bean
    public MappedInterceptor interceptorUsuarioLogado(UsuarioRepository usuarios) {
        return new MappedInterceptor(new String[]{"/**"}, new InterceptorUsuarioLogado(usuarios));
    }
}
