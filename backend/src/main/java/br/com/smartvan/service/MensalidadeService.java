package br.com.smartvan.service;

import br.com.smartvan.dto.MensalidadeResponseDTO;
import br.com.smartvan.enums.StatusContrato;
import br.com.smartvan.enums.StatusMensalidade;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.event.ContratoAtivadoEvent;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Contrato;
import br.com.smartvan.model.Mensalidade;
import br.com.smartvan.repository.ContratoRepository;
import br.com.smartvan.repository.MensalidadeRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

//geração, atraso, baixa manual e listagens das mensalidades (UC10)
@Service
public class MensalidadeService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private final MensalidadeRepository mensalidades;
    private final ContratoRepository contratos;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public MensalidadeService(MensalidadeRepository mensalidades, ContratoRepository contratos,
                              NotificacaoService notificacoes, AuditoriaService auditoria, Clock clock) {
        this.mensalidades = mensalidades;
        this.contratos = contratos;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    /**
     * Roda na transação do aceite: o erro sobe e desfaz tudo, e o contrato continua PENDENTE sem mensalidade nem
     * notificação; o motorista pode aceitar de novo. Não fica contrato ATIVO sem a primeira mensalidade.
     */
    @EventListener
    @Transactional
    public void aoAtivarContrato(ContratoAtivadoEvent evento) {
        gerarCompetenciaAtual(evento.contratoId());
    }

    /**
     * Gera a competência atual do contrato ATIVO quando chega o vencimento dela, se ainda não existir, e notifica.
     * Não gera se o contrato ainda não começou, se encerra até hoje (seria um mês sem serviço) ou se o aluno já tem
     * cobrança nessa competência por outro contrato, como na troca de motorista. Devolve true se criou.
     */
    @Transactional
    public boolean gerarCompetenciaAtual(Long contratoId) {
        Contrato c = contratos.carregar(contratoId).orElseThrow(() -> RegraNegocioException.naoEncontrado("Contrato"));
        LocalDate hoje = LocalDate.now(clock);
        LocalDate competencia = hoje.withDayOfMonth(1);
        //o contrato pode ter sido encerrado depois que o agendador leu a lista de ativos
        LocalDate vencimento = calcularVencimento(c.getDataInicio(), competencia);
        boolean encerraAteHoje = c.getDataEncerramentoEfetivo() != null && !c.getDataEncerramentoEfetivo().isAfter(hoje);
        if (c.getStatus() != StatusContrato.ATIVO || hoje.isBefore(c.getDataInicio()) || hoje.isBefore(vencimento)
                || encerraAteHoje || mensalidades.existsByContratoAlunoIdAndCompetencia(c.getAluno().getId(), competencia)) {
            return false;
        }

        Mensalidade m = new Mensalidade();
        m.setContrato(c);
        m.setCompetencia(competencia);
        m.setValor(c.getValorMensal());
        m.setVencimento(vencimento);
        m.setStatus(StatusMensalidade.PENDENTE);
        //flush aqui: se o unique contrato/competência barrar, o erro aparece antes de notificar
        mensalidades.saveAndFlush(m);

        notificacoes.notificar(c.getResponsavel().getId(), TipoNotificacao.MENSALIDADE_GERADA, "Mensalidade gerada",
                "A mensalidade de " + competencia.format(MES) + " de " + c.getAluno().getNome() + " vence em "
                        + m.getVencimento().format(DATA) + ".", m.getId());
        return true;
    }

    //todo mês no dia da assinatura; em mês mais curto, no último dia
    private static LocalDate calcularVencimento(LocalDate dataInicio, LocalDate competencia) {
        return competencia.withDayOfMonth(Math.min(dataInicio.getDayOfMonth(), competencia.lengthOfMonth()));
    }

    @Transactional(readOnly = true)
    public List<Long> idsContratosAtivos() {
        return contratos.idsComStatus(StatusContrato.ATIVO);
    }

    @Transactional(readOnly = true)
    public List<Long> idsPendentesVencidas() {
        return mensalidades.idsPendentesVencidas(LocalDate.now(clock));
    }

    /** Vira ATRASADA e notifica só se ainda estiver PENDENTE e vencida. Devolve true se mudou. */
    @Transactional
    public boolean marcarAtrasada(Long mensalidadeId) {
        Mensalidade m = mensalidades.travar(mensalidadeId)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Mensalidade"));
        //a baixa pode ter entrado depois que o agendador leu a lista
        if (m.getStatus() != StatusMensalidade.PENDENTE || !m.getVencimento().isBefore(LocalDate.now(clock))) {
            return false;
        }
        m.setStatus(StatusMensalidade.ATRASADA);
        Contrato c = m.getContrato();
        notificacoes.notificar(c.getResponsavel().getId(), TipoNotificacao.MENSALIDADE_ATRASADA,
                "Mensalidade em atraso", "A mensalidade de " + c.getAluno().getNome() + " com vencimento em "
                        + m.getVencimento().format(DATA) + " está em atraso.", m.getId());
        return true;
    }

    @Transactional
    public MensalidadeResponseDTO darBaixa(Long motoristaId, Long mensalidadeId, LocalDate dataPagamento) {
        Mensalidade m = mensalidades.travar(mensalidadeId)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Mensalidade"));
        Contrato c = m.getContrato();
        if (!c.getMotorista().getId().equals(motoristaId)) {
            throw RegraNegocioException.semPermissao();
        }
        if (m.getStatus() == StatusMensalidade.PAGA) {
            throw RegraNegocioException.conflito("MENSALIDADE_JA_PAGA", "Esta mensalidade já foi paga.");
        }
        LocalDate hoje = LocalDate.now(clock);
        LocalDate data = dataPagamento != null ? dataPagamento : hoje;
        if (data.isAfter(hoje)) {
            throw RegraNegocioException.regra("DATA_FUTURA", "A data de pagamento não pode ser futura.");
        }

        m.setStatus(StatusMensalidade.PAGA);
        m.setDataPagamento(data);
        auditoria.registrar(motoristaId, "BAIXA_MENSALIDADE", "Mensalidade", m.getId(),
                "Baixa manual: competência " + m.getCompetencia() + ", valor " + m.getValor() + ", pago em " + data);
        notificacoes.notificar(c.getResponsavel().getId(), TipoNotificacao.MENSALIDADE_PAGA, "Pagamento registrado",
                "O pagamento da mensalidade de " + c.getAluno().getNome() + " foi registrado em "
                        + data.format(DATA) + ".", m.getId());
        return MensalidadeResponseDTO.de(m);
    }

    @Transactional(readOnly = true)
    public List<MensalidadeResponseDTO> listarDoContrato(Long usuarioId, Long contratoId) {
        Contrato c = contratos.findById(contratoId)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Contrato"));
        if (!c.getResponsavel().getId().equals(usuarioId) && !c.getMotorista().getId().equals(usuarioId)) {
            throw RegraNegocioException.semPermissao();
        }
        return paraResposta(mensalidades.doContrato(contratoId));
    }

    @Transactional(readOnly = true)
    public List<MensalidadeResponseDTO> listarDoResponsavel(Long responsavelId, StatusMensalidade status) {
        return paraResposta(status == null
                ? mensalidades.doResponsavel(responsavelId)
                : mensalidades.doResponsavelComStatus(responsavelId, status));
    }

    @Transactional(readOnly = true)
    public List<MensalidadeResponseDTO> listarCobrancas(Long motoristaId, StatusMensalidade status,
                                                        YearMonth competencia) {
        List<Mensalidade> lista;
        if (competencia == null) {
            lista = status == null
                    ? mensalidades.doMotorista(motoristaId)
                    : mensalidades.doMotoristaComStatus(motoristaId, status);
        } else {
            lista = status == null
                    ? mensalidades.doMotoristaComCompetencia(motoristaId, competencia.atDay(1))
                    : mensalidades.doMotoristaComStatusECompetencia(motoristaId, status, competencia.atDay(1));
        }
        return paraResposta(lista);
    }

    private static List<MensalidadeResponseDTO> paraResposta(List<Mensalidade> lista) {
        return lista.stream().map(MensalidadeResponseDTO::de).toList();
    }
}
