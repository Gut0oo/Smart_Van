package br.com.smartvan.model;

import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.enums.TipoUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email")
    private String email;
    @Column(name = "senha")
    private String senha;

    @Column(name = "nome")
    private String nome;
    @Column(name = "cpf")
    private String cpf;
    @Column(name = "telefone")
    private String telefone;

    @Column(name = "tipo")
    private TipoUsuario tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusConta status;

    public Usuario(){}

    public boolean validarEmail(){ return email != null && email.contains("@") && email.contains(".") && !email.isBlank(); }

    public void ativarConta(){ this.status = StatusConta.ATIVA; }
    public void desativarConta(){ this.status = StatusConta.BLOQUEADA; }
    public boolean estaAtiva(){ return this.status == StatusConta.ATIVA; }


    @Override
    public String toString() {
        return "Usuario{" +
                "cpf='" + cpf + '\'' +
                ", email='" + email + '\'' +
                ", id=" + id +
                ", nome='" + nome + '\'' +
                ", status=" + status +
                ", telefone='" + telefone + '\'' +
                ", tipo=" + tipo +
                '}';
    }
}
