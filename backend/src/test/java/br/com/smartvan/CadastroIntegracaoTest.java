package br.com.smartvan;

import br.com.smartvan.enums.StatusVinculo;
import br.com.smartvan.enums.TipoEndereco;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.suporte.FakeMapsProvider;
import br.com.smartvan.suporte.TesteIntegracaoBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CadastroIntegracaoTest extends TesteIntegracaoBase {

    @Autowired JdbcTemplate banco;

    private Map<String, Object> endereco(boolean comCoordenadas) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("logradouro", "Rua Mourato Coelho");
        e.put("numero", "500");
        e.put("bairro", "Vila Madalena");
        e.put("cidade", "São Paulo");
        e.put("uf", "SP");
        e.put("cep", "05417001");
        if (comCoordenadas) {
            e.put("latitude", -23.5586);
            e.put("longitude", -46.6948);
        }
        return e;
    }

    private Map<String, Object> aluno(Long escolaId, boolean comCoordenadas) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("nome", "Lucas");
        a.put("dataNascimento", "2015-03-12");
        a.put("serie", "4º ano");
        a.put("escolaId", escolaId);
        a.put("parentesco", "MAE");
        a.put("endereco", endereco(comCoordenadas));
        return a;
    }

    private Map<String, Object> veiculo(String placa, int capacidade) {
        return Map.of("placa", placa, "modelo", "Sprinter", "ano", 2021, "capacidade", capacidade, "tipo", "VAN");
    }

    private Map<String, Object> atendimento(Long escolaId, String inicioAula, String saida) {
        return Map.of("escolaId", escolaId, "horarioInicioAula", inicioAula, "horarioSaidaMotorista", saida);
    }

    private Map<String, Object> atendimentos(Object... itens) {
        return Map.of("atendimentos", List.of(itens));
    }

    // ---------------------------------------------------------------- escolas

    @Test
    void listaEscolasSemLoginComFiltrosDeCidadeENome() throws Exception {
        fabrica.escola("Colégio Girassol", "São Paulo", "-23.56", "-46.69");
        fabrica.escola("Escola Aquarela", "São Paulo", "-23.55", "-46.69");
        fabrica.escola("Colégio Sol", "Campinas", "-22.90", "-47.06");

        fazGet("/escolas").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].endereco.latitude").exists());
        fazGet("/escolas?cidade=campinas").andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Colégio Sol"));
        fazGet("/escolas?nome=aquarela&cidade=são").andExpect(jsonPath("$.length()").value(1));
        fazGet("/escolas?nome=nada").andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------- alunos

    @Test
    void cadastroDeAlunoGravaAsTresTabelas() throws Exception {
        Responsavel r = fabrica.responsavel();
        Escola escola = fabrica.escola();

        fazPost("/responsavel/alunos", aluno(escola.getId(), true), r)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Lucas"))
                .andExpect(jsonPath("$.parentesco").value("MAE"))
                .andExpect(jsonPath("$.escola.id").value(escola.getId()))
                .andExpect(jsonPath("$.escola.nome").value(escola.getNome()))
                .andExpect(jsonPath("$.endereco.id").isNumber())
                .andExpect(jsonPath("$.endereco.latitude").value(-23.5586));

        assertThat(banco.queryForObject("select count(*) from alunos", Integer.class)).isEqualTo(1);
        Map<String, Object> ae = banco.queryForMap("select tipo, principal from alunos_enderecos");
        assertThat(ae.get("tipo")).isEqualTo(TipoEndereco.RESIDENCIAL.name());
        assertThat(ae.get("principal")).isEqualTo(true);
        Map<String, Object> ra = banco.queryForMap(
                "select responsavel_id, status_vinculo, acompanha_rota, parentesco from responsaveis_alunos");
        assertThat(((Number) ra.get("responsavel_id")).longValue()).isEqualTo(r.getId());
        assertThat(ra.get("status_vinculo")).isEqualTo(StatusVinculo.ATIVO.name());
        assertThat(ra.get("acompanha_rota")).isEqualTo(true);
        assertThat(ra.get("parentesco")).isEqualTo("MAE");
        assertThat(maps.geocodificacoes).isEmpty(); //veio com coordenadas
    }

    @Test
    void enderecoSemCoordenadaChamaGeocodificacaoESalvaOResultado() throws Exception {
        fazPost("/responsavel/alunos", aluno(fabrica.escola().getId(), false), fabrica.responsavel())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endereco.latitude")
                        .value(FakeMapsProvider.COORDENADA_PADRAO.latitude().doubleValue()))
                .andExpect(jsonPath("$.endereco.longitude")
                        .value(FakeMapsProvider.COORDENADA_PADRAO.longitude().doubleValue()));

        assertThat(maps.geocodificacoes).hasSize(1);
        assertThat(maps.geocodificacoes.getFirst())
                .contains("Rua Mourato Coelho", "500", "Vila Madalena", "São Paulo", "SP", "05417001");
    }

    @Test
    void semGeocodificacaoSalvaEnderecoSemCoordenada() throws Exception {
        maps.naoGeocodificar();

        fazPost("/responsavel/alunos", aluno(fabrica.escola().getId(), false), fabrica.responsavel())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endereco.id").isNumber())
                .andExpect(jsonPath("$.endereco.latitude").doesNotExist());
        assertThat(maps.geocodificacoes).hasSize(1);
    }

    @Test
    void alunoComEscolaInexistenteDevolve404() throws Exception {
        fazPost("/responsavel/alunos", aluno(999999L, true), fabrica.responsavel())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void alunoInvalidoDevolve400() throws Exception {
        Map<String, Object> a = aluno(fabrica.escola().getId(), true);
        a.put("nome", "");
        a.put("parentesco", null);
        ((Map<String, Object>) a.get("endereco")).put("cep", "123");
        ((Map<String, Object>) a.get("endereco")).put("latitude", 91);

        fazPost("/responsavel/alunos", a, fabrica.responsavel())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.parentesco").exists())
                .andExpect(jsonPath("$.campos['endereco.cep']").exists())
                .andExpect(jsonPath("$.campos['endereco.latitude']").exists());
    }

    @Test
    void listaSoOsAlunosDoResponsavelComEscolaEEndereco() throws Exception {
        Responsavel ana = fabrica.responsavel();
        Escola escola = fabrica.escola();
        fabrica.aluno(ana, escola);
        fabrica.aluno(ana, escola);
        fabrica.aluno(fabrica.responsavel(), escola);

        fazGet("/responsavel/alunos", ana)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].escola.nome").value(escola.getNome()))
                .andExpect(jsonPath("$[0].endereco.latitude").exists())
                .andExpect(jsonPath("$[0].parentesco").value("MAE"));
        fazGet("/responsavel/alunos", fabrica.responsavel()).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------- veículos

    @Test
    void motoristaCadastraEListaSoOsSeusVeiculos() throws Exception {
        Motorista m = fabrica.motorista();
        fazPost("/motorista/veiculos", veiculo("abc1d23", 15), m)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC1D23"))
                .andExpect(jsonPath("$.capacidade").value(15))
                .andExpect(jsonPath("$.tipo").value("VAN"));
        fabrica.veiculo(fabrica.motorista(), 10); //de outro motorista

        fazGet("/motorista/veiculos", m)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("ABC1D23"));
    }

    @Test
    void placaDuplicadaDevolve409() throws Exception {
        fazPost("/motorista/veiculos", veiculo("ABC1D23", 15), fabrica.motorista()).andExpect(status().isCreated());
        fazPost("/motorista/veiculos", veiculo("abc1d23", 10), fabrica.motorista())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PLACA_JA_CADASTRADA"));
    }

    @Test
    void capacidadeForaDoLimiteDevolve400() throws Exception {
        Motorista m = fabrica.motorista();
        fazPost("/motorista/veiculos", veiculo("AAA1A11", 0), m)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos.capacidade").exists());
        fazPost("/motorista/veiculos", veiculo("AAA1A12", 61), m).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- atendimentos

    @Test
    void motoristaTemVariosAtendimentosPorEscolaComTetoDeChegada() throws Exception {
        Motorista m = fabrica.motorista();
        Long escola = fabrica.escola("Escola A").getId();

        fazPut("/motorista/escolas", atendimentos(
                atendimento(escola, "13:00", "12:00"),
                atendimento(escola, "07:00", "06:10")), m)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].escola.nome").value("Escola A"))
                .andExpect(jsonPath("$[0].escola.endereco.cidade").value("São Paulo"))
                .andExpect(jsonPath("$[0].horarioInicioAula").value("07:00"))
                .andExpect(jsonPath("$[0].horarioSaidaMotorista").value("06:10"))
                .andExpect(jsonPath("$[0].tetoChegada").value("06:45"))
                .andExpect(jsonPath("$[1].horarioInicioAula").value("13:00"))
                .andExpect(jsonPath("$[1].tetoChegada").value("12:45"));

        fazGet("/motorista/escolas", m)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tetoChegada").value("06:45"));
    }

    @Test
    void putSubstituiOsAtendimentosSoDoProprioMotorista() throws Exception {
        Motorista m = fabrica.motorista();
        Motorista outro = fabrica.motorista();
        Escola a = fabrica.escola("Escola A");
        fabrica.atenderEscolas(m, a);
        fabrica.atenderEscolas(outro, a);
        Long c = fabrica.escola("Escola C").getId();

        fazPut("/motorista/escolas", atendimentos(atendimento(c, "07:30", "06:30")), m)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].escola.id").value(c));
        fazGet("/motorista/escolas", outro).andExpect(jsonPath("$.length()").value(1));

        fazPut("/motorista/escolas", atendimentos(), m).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        fazGet("/motorista/escolas", m).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void atendimentosIdenticosNoMesmoPutContamUmaVez() throws Exception {
        Long escola = fabrica.escola().getId();
        fazPut("/motorista/escolas", atendimentos(
                atendimento(escola, "07:00", "06:00"),
                atendimento(escola, "07:00", "06:00")), fabrica.motorista())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void atendimentoComEscolaInexistenteDevolve404ESemApagarOsAnteriores() throws Exception {
        Motorista m = fabrica.motorista();
        fabrica.atenderEscolas(m, fabrica.escola());

        fazPut("/motorista/escolas", atendimentos(atendimento(999999L, "07:00", "06:00")), m)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        fazGet("/motorista/escolas", m).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void atendimentoInvalidoDevolve400() throws Exception {
        Motorista m = fabrica.motorista();
        Map<String, Object> semHorarios = new LinkedHashMap<>();
        semHorarios.put("escolaId", null);

        fazPut("/motorista/escolas", atendimentos(semHorarios), m)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos['atendimentos[0].escolaId']").exists())
                .andExpect(jsonPath("$.campos['atendimentos[0].horarioInicioAula']").exists())
                .andExpect(jsonPath("$.campos['atendimentos[0].horarioSaidaMotorista']").exists());
        fazPut("/motorista/escolas", Map.of(), m)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.atendimentos").exists());
        fazPut("/motorista/escolas", atendimentos(atendimento(fabrica.escola().getId(), "25:00", "06:00")), m)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    // ---------------------------------------------------------------- endereço de partida e perfil

    @Test
    void enderecoDePartidaGeocodificaEAtualizaNoMesmoRegistro() throws Exception {
        Motorista m = fabrica.motorista();
        long id = json.readTree(fazPut("/motorista/endereco-partida", endereco(false), m)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(FakeMapsProvider.COORDENADA_PADRAO.latitude().doubleValue()))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        assertThat(maps.geocodificacoes).hasSize(1);

        Map<String, Object> novo = endereco(true);
        novo.put("numero", "999");
        fazPut("/motorista/endereco-partida", novo, m)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.numero").value("999"))
                .andExpect(jsonPath("$.latitude").value(-23.5586));
        assertThat(maps.geocodificacoes).hasSize(1); //com coordenadas não geocodifica de novo
    }

    @Test
    void enderecoDePartidaInvalidoDevolve400() throws Exception {
        Map<String, Object> e = endereco(true);
        e.put("uf", "sp");
        fazPut("/motorista/endereco-partida", e, fabrica.motorista())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.uf").exists());
    }

    @Test
    void perfilReuneUsuarioPartidaVeiculosEEscolasSemRepetir() throws Exception {
        Motorista m = fabrica.motorista();
        Escola escola = fabrica.escola("Escola Perfil");
        fabrica.veiculo(m, 12);
        fabrica.atendimento(m, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        fabrica.atendimento(m, escola, LocalTime.of(13, 0), LocalTime.of(12, 0));
        fabrica.enderecoPartida(m);

        fazGet("/motorista/perfil", m)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value(m.getId()))
                .andExpect(jsonPath("$.usuario.statusValidacao").value("VALIDADO"))
                .andExpect(jsonPath("$.enderecoPartida.latitude").exists())
                .andExpect(jsonPath("$.veiculos.length()").value(1))
                .andExpect(jsonPath("$.escolas.length()").value(1))
                .andExpect(jsonPath("$.escolas[0].nome").value("Escola Perfil"));
    }

    @Test
    void perfilSemPartidaOmiteOCampo() throws Exception {
        fazGet("/motorista/perfil", fabrica.motorista())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enderecoPartida").doesNotExist())
                .andExpect(jsonPath("$.veiculos.length()").value(0))
                .andExpect(jsonPath("$.escolas.length()").value(0));
    }

    // ---------------------------------------------------------------- papéis

    @Test
    void rotaDeOutroPapelDevolve403() throws Exception {
        Usuario responsavel = fabrica.responsavel();
        Usuario motorista = fabrica.motorista();

        for (String url : List.of("/motorista/veiculos", "/motorista/escolas", "/motorista/perfil")) {
            fazGet(url, responsavel)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        }
        fazPost("/motorista/veiculos", veiculo("ABC1D23", 15), responsavel).andExpect(status().isForbidden());
        fazPut("/motorista/escolas", atendimentos(), responsavel).andExpect(status().isForbidden());
        fazPut("/motorista/endereco-partida", endereco(true), responsavel).andExpect(status().isForbidden());

        fazGet("/responsavel/alunos", motorista)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        fazPost("/responsavel/alunos", aluno(1L, true), motorista).andExpect(status().isForbidden());
    }

    @Test
    void rotaComPapelSemIdentificacaoDevolve401() throws Exception {
        fazGet("/motorista/perfil")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        fazGet("/responsavel/alunos").andExpect(status().isUnauthorized());
    }
}
