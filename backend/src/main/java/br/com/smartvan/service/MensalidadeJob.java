package br.com.smartvan.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador diário das mensalidades. Fica sem transação de propósito: cada contrato e cada mensalidade têm a sua,
 * aberta no service, e o erro em um não desfaz nem interrompe os outros (o próximo dia tenta de novo).
 */
@Slf4j
@Component
public class MensalidadeJob {

    private final MensalidadeService service;

    public MensalidadeJob(MensalidadeService service) {
        this.service = service;
    }

    @Scheduled(cron = "${smartvan.agendador.mensalidades.cron}", zone = "America/Sao_Paulo")
    public void executar() {
        gerarCompetenciaAtual();
        marcarAtrasadas();
    }

    /** Gera a competência atual de cada contrato ATIVO que ainda não tem. Devolve quantas criou. */
    public int gerarCompetenciaAtual() {
        int criadas = 0;
        for (Long contratoId : service.idsContratosAtivos()) {
            try {
                if (service.gerarCompetenciaAtual(contratoId)) {
                    criadas++;
                }
            } catch (RuntimeException e) {
                log.error("Falha ao gerar a mensalidade do contrato {}", contratoId, e);
            }
        }
        return criadas;
    }

    /** Marca ATRASADA cada PENDENTE vencida e notifica uma vez. Devolve quantas mudaram. */
    public int marcarAtrasadas() {
        int marcadas = 0;
        for (Long mensalidadeId : service.idsPendentesVencidas()) {
            try {
                if (service.marcarAtrasada(mensalidadeId)) {
                    marcadas++;
                }
            } catch (RuntimeException e) {
                log.error("Falha ao marcar a mensalidade {} como atrasada", mensalidadeId, e);
            }
        }
        return marcadas;
    }
}
