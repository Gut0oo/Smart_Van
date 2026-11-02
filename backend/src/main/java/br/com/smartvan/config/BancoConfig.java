package br.com.smartvan.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Sem a variável DB_PASSWORD, a aplicação não sobe e diz o motivo. Sem esta checagem, o Spring usaria o texto
 * "${DB_PASSWORD}" como senha e o erro seria só uma falha de autenticação no banco.
 */
@Configuration
public class BancoConfig {

    private static final String MENSAGEM = "Defina a variável de ambiente DB_PASSWORD com a senha do banco de dados.";

    //roda antes da criação dos beans, portanto antes de qualquer tentativa de conexão
    @Bean
    static BeanFactoryPostProcessor exigirSenhaDoBanco(Environment ambiente) {
        return fabrica -> {
            try {
                ambiente.getProperty("spring.datasource.password");
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(MENSAGEM, e);
            }
        };
    }
}
