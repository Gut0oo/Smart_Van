package br.com.smartvan.suporte;

import br.com.smartvan.enums.Parentesco;
import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.enums.StatusVinculo;
import br.com.smartvan.enums.TipoEndereco;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.enums.TipoVeiculo;
import br.com.smartvan.model.Administrador;
import br.com.smartvan.model.Aluno;
import br.com.smartvan.model.AlunoEndereco;
import br.com.smartvan.model.Atendimento;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Endereco;
import br.com.smartvan.model.Escola;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.ResponsavelAluno;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.model.Veiculo;
import br.com.smartvan.repository.AlunoEnderecoRepository;
import br.com.smartvan.repository.AlunoRepository;
import br.com.smartvan.repository.AtendimentoRepository;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.EnderecoRepository;
import br.com.smartvan.repository.EscolaRepository;
import br.com.smartvan.repository.MotoristaRepository;
import br.com.smartvan.repository.ResponsavelAlunoRepository;
import br.com.smartvan.repository.ResponsavelRepository;
import br.com.smartvan.repository.UsuarioRepository;
import br.com.smartvan.repository.VeiculoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cria entidades direto pelos repositories (sem passar pela API). Cada método salva e devolve a entidade com id.
 * E-mails e CPFs são gerados únicos. Senha de todos os usuários: {@link #SENHA}.
 * As entidades devolvidas estão destacadas (sem sessão): use os ids e os repositories para reler.
 *
 * Cada etapa acrescenta aqui o que criar (contrato, mensalidade...).
 */
@Component
public class Fabrica {

    public static final String SENHA = "smartvan123";

    private final AtomicLong seq = new AtomicLong(1000);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final EnderecoRepository enderecos;
    private final EscolaRepository escolas;
    private final AlunoRepository alunos;
    private final AlunoEnderecoRepository alunosEnderecos;
    private final ResponsavelAlunoRepository vinculos;
    private final ResponsavelRepository responsaveis;
    private final MotoristaRepository motoristas;
    private final VeiculoRepository veiculos;
    private final AtendimentoRepository atendimentos;
    private final ContratoRepository contratos;

    private String hashSenha;

    public Fabrica(UsuarioRepository usuarios, PasswordEncoder encoder, EnderecoRepository enderecos,
                   EscolaRepository escolas, AlunoRepository alunos, AlunoEnderecoRepository alunosEnderecos,
                   ResponsavelAlunoRepository vinculos, ResponsavelRepository responsaveis,
                   MotoristaRepository motoristas, VeiculoRepository veiculos, AtendimentoRepository atendimentos,
                   ContratoRepository contratos) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.enderecos = enderecos;
        this.escolas = escolas;
        this.alunos = alunos;
        this.alunosEnderecos = alunosEnderecos;
        this.vinculos = vinculos;
        this.responsaveis = responsaveis;
        this.motoristas = motoristas;
        this.veiculos = veiculos;
        this.atendimentos = atendimentos;
        this.contratos = contratos;
    }

    private long proximo() {
        return seq.incrementAndGet();
    }

    //BCrypt é lento: calcula uma vez só
    private String hash() {
        if (hashSenha == null) {
            hashSenha = encoder.encode(SENHA);
        }
        return hashSenha;
    }

    // ---------------------------------------------------------------- usuários (conta ATIVA)

    public Responsavel responsavel() {
        return responsavel("Responsável " + proximo());
    }

    public Responsavel responsavel(String nome) {
        long n = proximo();
        Responsavel r = new Responsavel();
        preencher(r, nome, "resp" + n + "@teste.com", n, TipoUsuario.RESPONSAVEL);
        return usuarios.save(r);
    }

    /** Motorista VALIDADO. */
    public Motorista motorista() {
        return motorista(StatusMotorista.VALIDADO);
    }

    public Motorista motorista(StatusMotorista status) {
        return motorista("Motorista " + proximo(), status);
    }

    public Motorista motorista(String nome, StatusMotorista status) {
        long n = proximo();
        Motorista m = new Motorista();
        preencher(m, nome, "mot" + n + "@teste.com", n, TipoUsuario.MOTORISTA);
        m.setStatusValidacao(status);
        return usuarios.save(m);
    }

    public Administrador administrador() {
        long n = proximo();
        Administrador a = new Administrador();
        preencher(a, "Administrador " + n, "adm" + n + "@teste.com", n, TipoUsuario.ADMINISTRADOR);
        return usuarios.save(a);
    }

    private void preencher(Usuario u, String nome, String email, long n, TipoUsuario tipo) {
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(hash());
        u.setCpf(String.format("%011d", n));
        u.setTelefone("11" + String.format("%09d", n));
        u.setTipo(tipo);
        u.setStatus(StatusConta.ATIVA);
    }

    // ---------------------------------------------------------------- endereços e escolas

    /** Endereço em Pinheiros (SP) com coordenadas. */
    public Endereco endereco() {
        return endereco("-23.5670000", "-46.7020000");
    }

    /** Endereço com as coordenadas dadas (null = sem coordenadas). */
    public Endereco endereco(String latitude, String longitude) {
        return enderecos.save(montarEndereco(latitude, longitude));
    }

    private Endereco montarEndereco(String latitude, String longitude) {
        long n = proximo();
        Endereco e = new Endereco();
        e.setLogradouro("Rua Teste " + n);
        e.setNumero(String.valueOf(n % 1000));
        e.setBairro("Pinheiros");
        e.setCidade("São Paulo");
        e.setUf("SP");
        e.setCep("05422001");
        e.setLatitude(latitude == null ? null : new BigDecimal(latitude));
        e.setLongitude(longitude == null ? null : new BigDecimal(longitude));
        return e;
    }

    /** Escola em São Paulo com coordenadas. */
    public Escola escola() {
        return escola("Escola " + proximo());
    }

    public Escola escola(String nome) {
        return escola(nome, "São Paulo", "-23.5600000", "-46.6900000");
    }

    public Escola escola(String nome, String cidade, String latitude, String longitude) {
        Endereco e = montarEndereco(latitude, longitude);
        e.setCidade(cidade);
        Escola escola = new Escola();
        escola.setNome(nome);
        escola.setTelefone("1130000000");
        escola.setEndereco(enderecos.save(e));
        return escolas.save(escola);
    }

    // ---------------------------------------------------------------- aluno

    /** Aluno do responsável, na escola, com endereço residencial principal em Pinheiros (com coordenadas). */
    public Aluno aluno(Responsavel responsavel, Escola escola) {
        return aluno(responsavel, escola, endereco());
    }

    /** Idem, com o endereço residencial principal à escolha. */
    @Transactional
    public Aluno aluno(Responsavel responsavel, Escola escola, Endereco enderecoResidencial) {
        Aluno a = new Aluno();
        a.setNome("Aluno " + proximo());
        a.setDataNascimento(LocalDate.of(2015, 5, 20));
        a.setSerie("3º ano");
        a.setEscola(escolas.getReferenceById(escola.getId()));
        a = alunos.save(a);

        AlunoEndereco ae = new AlunoEndereco();
        ae.setAluno(a);
        ae.setEndereco(enderecos.getReferenceById(enderecoResidencial.getId()));
        ae.setTipo(TipoEndereco.RESIDENCIAL);
        ae.setPrincipal(true);
        alunosEnderecos.save(ae);

        ResponsavelAluno v = new ResponsavelAluno();
        v.setResponsavel(responsaveis.getReferenceById(responsavel.getId()));
        v.setAluno(a);
        v.setParentesco(Parentesco.MAE);
        v.setStatusVinculo(StatusVinculo.ATIVO);
        v.setAcompanhaRota(true);
        vinculos.save(v);
        return a;
    }

    // ---------------------------------------------------------------- motorista: veículo, atendimentos, partida

    public Veiculo veiculo(Motorista motorista, int capacidade) {
        String placa = "TST" + String.format("%04d", proximo() % 10000);
        return veiculos.save(new Veiculo(motorista, TipoVeiculo.VAN, placa, "Van Teste", capacidade, 2022));
    }

    public Atendimento atendimento(Motorista motorista, Escola escola, LocalTime inicioAula, LocalTime saidaMotorista) {
        Atendimento a = new Atendimento();
        a.setMotorista(motorista);
        a.setEscola(escola);
        a.setHorarioInicioAula(inicioAula);
        a.setHorarioSaidaMotorista(saidaMotorista);
        return atendimentos.save(a);
    }

    /** Um atendimento em cada escola, com aula às 07:00 e saída às 06:00. */
    public void atenderEscolas(Motorista motorista, Escola... escolasAtendidas) {
        for (Escola escola : escolasAtendidas) {
            atendimento(motorista, escola, LocalTime.of(7, 0), LocalTime.of(6, 0));
        }
    }

    // ---------------------------------------------------------------- contrato

    /** Contrato direto no banco, com a mensalidade de 350,00 e início em 2026-11-03. */
    public Contrato contrato(Responsavel responsavel, Aluno aluno, Atendimento atendimento, StatusContrato status) {
        Contrato c = new Contrato();
        c.setResponsavel(responsavel);
        c.setMotorista(atendimento.getMotorista());
        c.setAluno(aluno);
        c.setAtendimento(atendimento);
        c.setValorMensal(new BigDecimal("350.00"));
        c.setDataInicio(LocalDate.of(2026, 11, 3));
        c.setStatus(status);
        return contratos.save(c);
    }

    /** Define o endereço de partida do motorista (com coordenadas). Devolve o endereço. */
    @Transactional
    public Endereco enderecoPartida(Motorista motorista) {
        Endereco e = endereco("-23.5630000", "-46.6970000");
        motoristas.findById(motorista.getId()).orElseThrow().setEnderecoPartida(e);
        return e;
    }
}
