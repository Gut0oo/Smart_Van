package br.com.smartvan.suporte;

import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.enums.TipoUsuario;
import br.com.smartvan.model.Administrador;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Responsavel;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Cria entidades direto pelos repositories (sem passar pela API). Cada método salva e devolve a entidade com id.
 * E-mails e CPFs são gerados únicos. Senha de todos os usuários: {@link #SENHA}.
 * As entidades devolvidas estão destacadas (sem sessão): use os ids e os repositories para reler.
 *
 * Etapa 1: só usuários (os outros repositories ainda não existem). Cada etapa acrescenta aqui o que criar
 * (endereço, escola, aluno, veículo, contrato, mensalidade...).
 */
@Component
public class Fabrica {

    public static final String SENHA = "smartvan123";

    private final AtomicLong seq = new AtomicLong(1000);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    private String hashSenha;

    public Fabrica(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
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
}
