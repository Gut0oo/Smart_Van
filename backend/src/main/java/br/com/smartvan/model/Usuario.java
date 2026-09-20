package br.com.smartvan.model;

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

    @Column(name = "nome")
    private String nome;

    @Column(name = "senha")
    private String senha;

    @Column(name = "tipo")
    private TipoUsuario tipo;

    public Usuario(){}

    @Override
    public String toString(){
        return "Usuario{" + "id='" + id + '\'' +
                ", nome= '" + nome + '\'' +
                ", descricao= '" + email + '\'' +
                "}";
    }
}
