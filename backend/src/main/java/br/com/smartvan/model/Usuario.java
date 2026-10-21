package br.com.smartvan.model;

import br.com.smartvan.enums.StatusConta;
import br.com.smartvan.enums.TipoUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter

@Entity
@Inheritance(strategy = InheritanceType.JOINED)//para o bd tratar as heranças (motorista, responsavel, adm)
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;
    @Column(name = "senha_hash", nullable = false)
    private String senha;

    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "cpf", length = 11, unique = true, nullable = false)
    private String cpf;
    @Column(name = "telefone", length = 20, nullable = false)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoUsuario tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private StatusConta status;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @Column(name = "ultimo_acesso")
    private Instant ultimoAcesso;


    public Usuario(){}

    public boolean validarEmail(){ return email != null && email.contains("@") && email.contains(".") && !email.isBlank(); }
    public void ativarConta(){ this.status = StatusConta.ATIVA; }
    public void desativarConta(){ this.status = StatusConta.BLOQUEADA; }
    public boolean estaAtiva(){ return this.status == StatusConta.ATIVA; }
}
