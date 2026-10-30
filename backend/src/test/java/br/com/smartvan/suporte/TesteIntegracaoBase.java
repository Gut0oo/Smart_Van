package br.com.smartvan.suporte;

import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.model.Auditoria;
import br.com.smartvan.model.Notificacao;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.AuditoriaRepository;
import br.com.smartvan.repository.NotificacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Base dos testes de integração: H2 em modo PostgreSQL, Clock fixo ({@link RelogioMutavel}) e
 * MapsProvider falso ({@link FakeMapsProvider}). Antes de cada teste: limpa todas as tabelas, reinicia o
 * relógio e o FakeMapsProvider.
 *
 * Etapa 1: ainda não há login, então os atalhos de MockMvc não mandam token. A etapa 2 acrescenta
 * a autenticação (ex.: {@code autenticar(req, usuario)} com o helper de sessão).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({RelogioFixoConfig.class, MapsFalsoConfig.class})
public abstract class TesteIntegracaoBase {

    @Autowired protected MockMvc mvc;
    @Autowired protected Fabrica fabrica;
    @Autowired protected RelogioMutavel relogio;
    @Autowired protected FakeMapsProvider maps;
    @Autowired protected JsonMapper json;
    @Autowired protected NotificacaoRepository notificacaoRepository;
    @Autowired protected AuditoriaRepository auditoriaRepository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void reiniciarAntesDeCadaTeste() {
        limparBanco();
        relogio.reiniciar();
        maps.reiniciar();
    }

    /**
     * Apaga as linhas de todas as tabelas do schema (já roda antes de cada teste). Lê a lista de tabelas do
     * próprio banco, então novas entidades das próximas etapas entram sem mexer aqui.
     */
    protected void limparBanco() {
        List<String> tabelas = jdbc.queryForList(
                "select table_name from information_schema.tables "
                        + "where lower(table_schema) = 'public' and table_type = 'BASE TABLE'", String.class);
        jdbc.execute("set referential_integrity false");
        try {
            tabelas.forEach(tabela -> jdbc.execute("truncate table " + tabela));
        } finally {
            jdbc.execute("set referential_integrity true");
        }
    }

    // ---------------------------------------------------------------- atalhos de MockMvc (sem token)

    protected ResultActions fazGet(String url) throws Exception {
        return mvc.perform(get(url));
    }

    protected ResultActions fazPost(String url, Object corpo) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }

    /** POST sem corpo (ex.: aceitar/recusar). */
    protected ResultActions fazPost(String url) throws Exception {
        return mvc.perform(post(url));
    }

    protected ResultActions fazPut(String url, Object corpo) throws Exception {
        return mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }

    protected ResultActions fazDelete(String url) throws Exception {
        return mvc.perform(delete(url));
    }

    // ---------------------------------------------------------------- verificação de efeitos

    /** Notificações do usuário (mais recentes primeiro). */
    protected List<Notificacao> notificacoesDe(Usuario usuario) {
        return notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDescIdDesc(usuario.getId());
    }

    protected List<Notificacao> notificacoesDe(Usuario usuario, TipoNotificacao tipo) {
        return notificacoesDe(usuario).stream().filter(n -> n.getTipo() == tipo).toList();
    }

    /** Auditorias da entidade, na ordem em que foram registradas. */
    protected List<Auditoria> auditorias(String entidade, Long entidadeId) {
        return auditoriaRepository.findByEntidadeAndEntidadeIdOrderByDataRegistroAscIdAsc(entidade, entidadeId);
    }
}
