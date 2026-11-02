package br.com.smartvan.service;

import br.com.smartvan.dto.UsuarioDetalheResponseDTO;
import br.com.smartvan.dto.ValidacaoRequestDTO;
import br.com.smartvan.enums.StatusMotorista;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Administrador;
import br.com.smartvan.model.Motorista;
import br.com.smartvan.model.Validacao;
import br.com.smartvan.repository.MotoristaRepository;
import br.com.smartvan.repository.ValidacaoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//o administrador aprova ou rejeita o motorista; só o VALIDADO aparece na busca e recebe proposta
@Service
public class ValidacaoMotoristaService {

    private static final List<StatusMotorista> SEM_DECISAO = List.of(StatusMotorista.PENDENTE, StatusMotorista.EM_ANALISE);

    private final MotoristaRepository motoristas;
    private final ValidacaoRepository validacoes;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final EntityManager entityManager;

    public ValidacaoMotoristaService(MotoristaRepository motoristas, ValidacaoRepository validacoes,
                                     NotificacaoService notificacoes, AuditoriaService auditoria,
                                     EntityManager entityManager) {
        this.motoristas = motoristas;
        this.validacoes = validacoes;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDetalheResponseDTO> listarPendentes() {
        return motoristas.findByStatusValidacaoInOrderByIdAsc(SEM_DECISAO).stream()
                .map(UsuarioDetalheResponseDTO::de).toList();
    }

    @Transactional
    public UsuarioDetalheResponseDTO decidir(Long administradorId, Long motoristaId, ValidacaoRequestDTO req) {
        boolean aprovado = req.decisao() == StatusMotorista.VALIDADO;
        if (!aprovado && req.decisao() != StatusMotorista.REJEITADO) {
            throw new RegraNegocioException(HttpStatus.BAD_REQUEST, "VALIDACAO", "A decisão deve ser VALIDADO ou REJEITADO.");
        }
        Motorista motorista = motoristas.findById(motoristaId)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Motorista"));
        if (!SEM_DECISAO.contains(motorista.getStatusValidacao())) {
            throw RegraNegocioException.conflito("MOTORISTA_JA_AVALIADO", "Este motorista já foi avaliado.");
        }
        String parecer = req.parecer() == null || req.parecer().isBlank() ? null : req.parecer().trim();
        if (!aprovado && parecer == null) {
            throw RegraNegocioException.regra("PARECER_OBRIGATORIO", "Informe o parecer ao rejeitar o motorista.");
        }

        motorista.setStatusValidacao(req.decisao());
        Validacao validacao = new Validacao();
        //referência sem SELECT: só precisa do id para a chave estrangeira
        validacao.setAdministrador(entityManager.getReference(Administrador.class, administradorId));
        validacao.setMotorista(motorista);
        validacao.setDecisao(req.decisao());
        validacao.setParecer(parecer);
        validacoes.save(validacao);
        auditoria.registrar(administradorId, "VALIDACAO_MOTORISTA", "Motorista", motoristaId, "Decisão: " + req.decisao());
        notificar(motorista, aprovado, parecer);
        return UsuarioDetalheResponseDTO.de(motorista);
    }

    //usa os tipos de proposta porque um tipo novo esbarra no CHECK de notificacoes.tipo dos bancos que já existem
    private void notificar(Motorista motorista, boolean aprovado, String parecer) {
        if (aprovado) {
            notificacoes.notificar(motorista.getId(), TipoNotificacao.PROPOSTA_ACEITA, "Cadastro aprovado",
                    "Seu cadastro foi aprovado. Agora você aparece na busca de motoristas.", null);
        } else {
            notificacoes.notificar(motorista.getId(), TipoNotificacao.PROPOSTA_RECUSADA, "Cadastro rejeitado",
                    "Seu cadastro foi rejeitado. Parecer: " + parecer, null);
        }
    }
}
