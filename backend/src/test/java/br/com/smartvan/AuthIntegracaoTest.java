package br.com.smartvan;

import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import br.com.smartvan.security.InterceptorUsuarioLogado;
import br.com.smartvan.security.Papel;
import br.com.smartvan.security.UsuarioLogado;
import br.com.smartvan.suporte.Fabrica;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Login, header X-Usuario-Id e controle de acesso (seção 1 do contrato). */
@Import({AuthIntegracaoTest.ControllerDePapeis.class, AuthIntegracaoTest.ControllerSoDeMotorista.class,
        AuthIntegracaoTest.ControllerGetUsuarios.class})
class AuthIntegracaoTest extends TesteIntegracaoBase {

    @Autowired UsuarioRepository usuarioRepository;

    private ResultActions login(String email, String senha) throws Exception {
        return fazPost("/auth/login", Map.of("email", email, "senha", senha));
    }

    private ResultActions comHeader(String url, String valor) throws Exception {
        return mvc.perform(get(url).header(InterceptorUsuarioLogado.HEADER, valor));
    }

    private void bloquear(Usuario usuario) {
        usuario.setStatus(StatusConta.BLOQUEADA);
        usuarioRepository.save(usuario);
    }

    private static void assertEstadoLimpo() {
        assertThatThrownBy(UsuarioLogado::atual)
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("NAO_AUTENTICADO"));
    }

    // ---------------------------------------------------------------- login

    @Test
    void loginCorretoDevolveSoOUsuarioSemSenha() throws Exception {
        Responsavel r = fabrica.responsavel();

        login(r.getEmail(), Fabrica.SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.expiraEm").doesNotExist())
                .andExpect(jsonPath("$.usuario.id").value(r.getId()))
                .andExpect(jsonPath("$.usuario.nome").value(r.getNome()))
                .andExpect(jsonPath("$.usuario.email").value(r.getEmail()))
                .andExpect(jsonPath("$.usuario.cpf").value(r.getCpf()))
                .andExpect(jsonPath("$.usuario.telefone").value(r.getTelefone()))
                .andExpect(jsonPath("$.usuario.tipo").value("RESPONSAVEL"))
                .andExpect(jsonPath("$.usuario.status").value("ATIVA"))
                .andExpect(jsonPath("$.usuario.statusValidacao").doesNotExist())
                .andExpect(jsonPath("$.usuario.senha").doesNotExist());
    }

    @Test
    void loginAtualizaUltimoAcesso() throws Exception {
        Responsavel r = fabrica.responsavel();
        assertThat(r.getUltimoAcesso()).isNull();

        login(r.getEmail(), Fabrica.SENHA).andExpect(status().isOk());

        assertThat(usuarioRepository.findById(r.getId()).orElseThrow().getUltimoAcesso())
                .isEqualTo(relogio.instant());
    }

    @Test
    void senhaErradaEEmailInexistenteDao401ComAMesmaMensagem() throws Exception {
        Responsavel r = fabrica.responsavel();

        String senhaErrada = login(r.getEmail(), "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andReturn().getResponse().getContentAsString();
        String emailInexistente = login("nao@existe.com", Fabrica.SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(senhaErrada).get("mensagem")).isEqualTo(json.readTree(emailInexistente).get("mensagem"));
        assertThat(usuarioRepository.findById(r.getId()).orElseThrow().getUltimoAcesso()).isNull();
    }

    @Test
    void contaBloqueadaNoLoginDa403SoComASenhaCerta() throws Exception {
        Responsavel r = fabrica.responsavel();
        bloquear(r);

        login(r.getEmail(), Fabrica.SENHA)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("CONTA_BLOQUEADA"));
        login(r.getEmail(), "errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
        assertThat(usuarioRepository.findById(r.getId()).orElseThrow().getUltimoAcesso()).isNull();
    }

    @Test
    void contaPendenteDoCadastroOriginalEntraEUsaAsRotas() throws Exception {
        String corpo = fazPost("/usuarios", Map.of("nome", "Novo Motorista", "email", "novo@teste.com",
                "senha", "segredo1", "cpf", "98765432100", "telefone", "11999990000", "tipo", "MOTORISTA"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString();

        login("novo@teste.com", "segredo1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.status").value("PENDENTE"))
                .andExpect(jsonPath("$.usuario.statusValidacao").value("PENDENTE"));
        comHeader("/auth/me", json.readTree(corpo).get("id").asString()).andExpect(status().isOk());
    }

    @Test
    void emailComCaixaDiferenteEntra() throws Exception {
        Responsavel r = fabrica.responsavel();

        login(r.getEmail().toUpperCase(), Fabrica.SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value(r.getId()));
    }

    @Test
    void emailsQueSoDiferemNaCaixaNaoQuebramOLogin() throws Exception {
        Responsavel minusculo = fabrica.responsavel();
        Responsavel maiusculo = fabrica.responsavel();
        maiusculo.setEmail(minusculo.getEmail().toUpperCase());
        usuarioRepository.save(maiusculo);

        login(minusculo.getEmail(), Fabrica.SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value(minusculo.getId()));
        login(minusculo.getEmail(), "errada").andExpect(status().isUnauthorized());
    }

    @Test
    void loginSemCamposDa400Validacao() throws Exception {
        fazPost("/auth/login", Map.of("email", "", "senha", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void logoutNaoExisteMais() throws Exception {
        mvc.perform(autenticar(post("/auth/logout"), fabrica.responsavel()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- /auth/me e header X-Usuario-Id

    @Test
    void meDevolveOUsuarioDoHeader() throws Exception {
        Responsavel r = fabrica.responsavel();

        fazGet("/auth/me", r)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(r.getId()))
                .andExpect(jsonPath("$.email").value(r.getEmail()))
                .andExpect(jsonPath("$.tipo").value("RESPONSAVEL"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void meDeMotoristaTrazStatusValidacao() throws Exception {
        Motorista m = fabrica.motorista(StatusMotorista.EM_ANALISE);

        fazGet("/auth/me", m)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("MOTORISTA"))
                .andExpect(jsonPath("$.statusValidacao").value("EM_ANALISE"));
    }

    @Test
    void headerAusenteDa401NoJsonPadrao() throws Exception {
        fazGet("/auth/me")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.mensagem").isString())
                .andExpect(jsonPath("$.timestamp").value("2026-11-03T12:00:00Z"));
    }

    @Test
    void headerInvalidoDa401() throws Exception {
        fabrica.responsavel();
        for (String valor : new String[]{"abc", "", "1.5", "99999999999999999999"}) {
            comHeader("/auth/me", valor)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        }
    }

    @Test
    void headerDeUsuarioInexistenteDa401() throws Exception {
        comHeader("/auth/me", "999999")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void headerDeContaBloqueadaDa403() throws Exception {
        Responsavel r = fabrica.responsavel();
        bloquear(r);

        fazGet("/auth/me", r)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("CONTA_BLOQUEADA"));
    }

    // ---------------------------------------------------------------- papéis

    @Test
    void rotaDeOutroPapelDa403NoJsonPadrao() throws Exception {
        fazGet("/teste-papeis/motorista", fabrica.responsavel())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"))
                .andExpect(jsonPath("$.timestamp").exists());
        fazGet("/teste-papeis/responsavel", fabrica.motorista())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        fazGet("/teste-papeis/motorista", fabrica.administrador())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void rotaComPapelSemHeaderDa401() throws Exception {
        fazGet("/teste-papeis/motorista")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void papelNoControllerValeParaTodasAsRotasEOMetodoPrevalece() throws Exception {
        Motorista m = fabrica.motorista();
        Responsavel r = fabrica.responsavel();

        fazGet("/teste-so-motorista", m).andExpect(status().isOk());
        fazGet("/teste-so-motorista", r)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        fazGet("/teste-so-motorista/excecao", r).andExpect(status().isOk());
        fazGet("/teste-so-motorista/excecao", m).andExpect(status().isForbidden());
    }

    @Test
    void usuarioLogadoAtualDevolveIdTipoEHelpers() throws Exception {
        Motorista m = fabrica.motorista();
        Responsavel r = fabrica.responsavel();

        fazGet("/teste-papeis/motorista", m)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(m.getId()))
                .andExpect(jsonPath("$.tipo").value("MOTORISTA"))
                .andExpect(jsonPath("$.ehMotorista").value(true))
                .andExpect(jsonPath("$.ehResponsavel").value(false));
        fazGet("/teste-papeis/responsavel", r)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(r.getId()))
                .andExpect(jsonPath("$.tipo").value("RESPONSAVEL"))
                .andExpect(jsonPath("$.ehMotorista").value(false))
                .andExpect(jsonPath("$.ehResponsavel").value(true));
    }

    // ---------------------------------------------------------------- limpeza do UsuarioLogado
    // o MockMvc roda a requisição na thread do teste, então dá para olhar o estado logo depois dela

    @Test
    void estadoELimpoAoFimDeRequisicaoComSucesso() throws Exception {
        Motorista m = fabrica.motorista();

        fazGet("/teste-papeis/motorista", m).andExpect(status().isOk());

        assertEstadoLimpo();
    }

    @Test
    void estadoELimpoQuandoARotaLancaExcecao() throws Exception {
        fazGet("/teste-papeis/com-erro", fabrica.responsavel()).andExpect(status().isConflict());

        assertEstadoLimpo();
    }

    @Test
    void estadoELimpoQuandoOPapelENegado() throws Exception {
        fazGet("/teste-papeis/motorista", fabrica.responsavel()).andExpect(status().isForbidden());

        assertEstadoLimpo();
    }

    @Test
    void estadoELimpoQuandoAContaEstaBloqueada() throws Exception {
        Responsavel r = fabrica.responsavel();
        bloquear(r);

        fazGet("/auth/me", r).andExpect(status().isForbidden());

        assertEstadoLimpo();
    }

    @Test
    void requisicaoSemHeaderNaoHerdaOUsuarioDaAnterior() throws Exception {
        fazGet("/auth/me", fabrica.responsavel()).andExpect(status().isOk());

        fazGet("/auth/me").andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- rotas públicas e proteção por padrão

    @Test
    void cadastroOriginalContinuaPublico() throws Exception {
        fazPost("/usuarios", Map.of("nome", "Nova Responsável", "email", "nova@teste.com", "senha", "segredo1",
                "cpf", "98765432101", "telefone", "11999990001", "tipo", "RESPONSAVEL"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nova@teste.com"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void healthEEscolasSaoPublicos() throws Exception {
        fazGet("/actuator/health").andExpect(status().isOk());
        //a rota de escolas entra na etapa 3; aqui só importa que o interceptor não a bloqueie
        fazGet("/escolas").andExpect(r -> assertThat(r.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void headerInvalidoNaoAtrapalhaRotaPublica() throws Exception {
        comHeader("/actuator/health", "abc").andExpect(status().isOk());
    }

    @Test
    void rotaPublicaSoValeParaOMetodoListado() throws Exception {
        fazGet("/usuarios")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        fazGet("/usuarios", fabrica.responsavel()).andExpect(status().isOk());
    }

    @Test
    void rotaSemConfiguracaoNenhumaEProtegidaPorPadrao() throws Exception {
        fazGet("/teste-papeis/sem-papel")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        fazGet("/teste-papeis/sem-papel", fabrica.responsavel()).andExpect(status().isOk());
    }

    @Test
    void rotaInexistenteSemHeaderDa401() throws Exception {
        fazGet("/rota-que-nao-existe")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void outrasRotasDoActuatorExigemIdentificacao() throws Exception {
        fazGet("/actuator")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    // ---------------------------------------------------------------- controllers só deste teste

    //as rotas reais com papel entram na etapa 3
    @RestController
    @RequestMapping("/teste-papeis")
    public static class ControllerDePapeis {

        @GetMapping("/motorista")
        @Papel(TipoUsuario.MOTORISTA)
        public Map<String, Object> motorista() {
            return resumo();
        }

        @GetMapping("/responsavel")
        @Papel(TipoUsuario.RESPONSAVEL)
        public Map<String, Object> responsavel() {
            return resumo();
        }

        @GetMapping("/sem-papel")
        public Map<String, Object> semPapel() {
            return resumo();
        }

        @GetMapping("/com-erro")
        public void comErro() {
            throw RegraNegocioException.conflito("CONFLITO_DE_TESTE", "Conflito de teste.");
        }

        private Map<String, Object> resumo() {
            UsuarioLogado eu = UsuarioLogado.atual();
            return Map.of("id", eu.id(), "tipo", eu.tipo(),
                    "ehMotorista", eu.ehMotorista(), "ehResponsavel", eu.ehResponsavel());
        }
    }

    //mesmo caminho do cadastro público, com outro método
    @RestController
    public static class ControllerGetUsuarios {

        @GetMapping("/usuarios")
        public void listar() {
        }
    }

    @RestController
    @RequestMapping("/teste-so-motorista")
    @Papel(TipoUsuario.MOTORISTA)
    public static class ControllerSoDeMotorista {

        @GetMapping
        public void rota() {
        }

        @GetMapping("/excecao")
        @Papel(TipoUsuario.RESPONSAVEL)
        public void excecao() {
        }
    }
}
