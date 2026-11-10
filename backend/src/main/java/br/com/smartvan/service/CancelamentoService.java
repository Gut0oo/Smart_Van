package br.com.smartvan.service;

import br.com.smartvan.dto.ContratoResponseDTO;
import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

//solicitação de cancelamento (vai até o próximo vencimento) e encerramento dos contratos vencidos
@Service
public class CancelamentoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratoRepository contratos;
    private final MensalidadeConsultaService mensalidades;
    private final NotificacaoService notificacoes;
    private final Clock clock;
    private final ContratoResposta resposta;

    public CancelamentoService(ContratoRepository contratos, MensalidadeConsultaService mensalidades,
                               NotificacaoService notificacoes, Clock clock, ContratoResposta resposta) {
        this.contratos = contratos;
        this.mensalidades = mensalidades;
        this.notificacoes = notificacoes;
        this.clock = clock;
        this.resposta = resposta;
    }

    @Transactional
    public ContratoResponseDTO solicitar(UsuarioLogado eu, Long contratoId, String motivo) {
        //trava antes de checar: outra solicitação ou o agendador esperam e veem o estado já gravado
        Contrato c = contratos.travar(contratoId).orElseThrow(() -> RegraNegocioException.naoEncontrado("Contrato"));
        Long responsavelId = c.getResponsavel().getId();
        Long motoristaId = c.getMotorista().getId();
        if (!responsavelId.equals(eu.id()) && !motoristaId.equals(eu.id())) {
            throw RegraNegocioException.semPermissao();
        }
        if (c.getStatus() != StatusContrato.ATIVO) {
            throw RegraNegocioException.conflito("CONTRATO_NAO_ATIVO", "O contrato não está ativo.");
        }
        if (c.getDataSolicitacaoCancelamento() != null) {
            throw RegraNegocioException.conflito("CANCELAMENTO_JA_SOLICITADO",
                    "O cancelamento deste contrato já foi solicitado.");
        }

        LocalDate hoje = LocalDate.now(clock);
        c.setDataSolicitacaoCancelamento(hoje);
        //vai até o próximo vencimento: usa o mês já pago até o fim, sem gerar mensalidade nova
        c.setDataEncerramentoEfetivo(VencimentoMensal.proximoDepoisDe(c.getDataInicio(), hoje));
        c.setSolicitanteCancelamento(eu.tipo());
        c.setMotivoCancelamento(motivo);

        Long outraParte = responsavelId.equals(eu.id()) ? motoristaId : responsavelId;
        notificacoes.notificar(outraParte, TipoNotificacao.CANCELAMENTO_SOLICITADO, "Cancelamento solicitado",
                "O contrato de " + c.getAluno().getNome() + " será encerrado em "
                        + c.getDataEncerramentoEfetivo().format(DATA) + ". Motivo: " + motivo, c.getId());
        return resposta.de(c);
    }

    @Transactional(readOnly = true)
    public List<Long> idsVencidos() {
        return contratos.idsAtivosComEncerramentoAte(LocalDate.now(clock));
    }

    /** Vira ENCERRADO e notifica as duas partes só se ainda estiver ATIVO e vencido. Devolve true se encerrou. */
    @Transactional
    public boolean encerrar(Long contratoId) {
        Contrato c = contratos.travar(contratoId).orElseThrow(() -> RegraNegocioException.naoEncontrado("Contrato"));
        LocalDate encerramento = c.getDataEncerramentoEfetivo();
        //o contrato pode ter mudado depois que o agendador leu a lista
        if (c.getStatus() != StatusContrato.ATIVO || encerramento == null || encerramento.isAfter(LocalDate.now(clock))) {
            return false;
        }
        c.setStatus(StatusContrato.ENCERRADO);

        String nomeAluno = c.getAluno().getNome();
        if (mensalidades.existePendenteOuAtrasada(contratoId)) {
            notificarPartes(c, TipoNotificacao.CANCELAMENTO_COM_PENDENCIA, "Contrato encerrado com pendência",
                    "O contrato de " + nomeAluno + " foi encerrado e há mensalidades pendentes ou atrasadas.");
        } else {
            notificarPartes(c, TipoNotificacao.CONTRATO_ENCERRADO, "Contrato encerrado",
                    "O contrato de " + nomeAluno + " foi encerrado.");
        }
        return true;
    }

    private void notificarPartes(Contrato c, TipoNotificacao tipo, String titulo, String mensagem) {
        notificacoes.notificar(c.getResponsavel().getId(), tipo, titulo, mensagem, c.getId());
        notificacoes.notificar(c.getMotorista().getId(), tipo, titulo, mensagem, c.getId());
    }
}
