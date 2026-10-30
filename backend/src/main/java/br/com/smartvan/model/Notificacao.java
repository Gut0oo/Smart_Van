package br.com.smartvan.model;

import br.com.smartvan.enums.TipoNotificacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

//notificação dentro do app (não há push nem e-mail); todos os módulos criam pelo NotificacaoService
@Getter
@Setter

@Entity
@Table(name = "notificacoes", indexes = @Index(name = "idx_notificacoes_usuario_lida", columnList = "usuario_id, lida"))
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacao_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 40, nullable = false)
    private TipoNotificacao tipo;

    @Column(name = "titulo", length = 120, nullable = false)
    private String titulo;

    @Column(name = "mensagem", columnDefinition = "text", nullable = false)
    private String mensagem;

    //id do recurso relacionado (contrato, mensalidade, rota...), quando houver
    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "lida", nullable = false)
    private boolean lida;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;
}
