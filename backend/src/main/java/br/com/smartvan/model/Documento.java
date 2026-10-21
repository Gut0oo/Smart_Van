package br.com.smartvan.model;

import br.com.smartvan.enums.StatusDocumento;
import br.com.smartvan.enums.TipoDocumento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter

@Entity
@Table(name = "documentos")
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    //so preenchido quando tipo = VISTORIA_VEICULO
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoDocumento tipo;

    //caminho do arquivo no bucket (S3), o arquivo em si nao fica no banco
    @Column(name = "chave_arquivo", nullable = false)
    private String chaveArquivo;

    @CreationTimestamp
    @Column(name = "data_envio", nullable = false, updatable = false)
    private Instant dataEnvio;

    @Column(name = "validade")
    private LocalDate validade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private StatusDocumento status;

    @Column(name = "motivo_recusa", columnDefinition = "text")
    private String motivoRecusa;
}
