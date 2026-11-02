package br.com.smartvan.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador diário dos cancelamentos. Fica sem transação de propósito: cada contrato tem a sua, aberta no service, e
 * o erro em um não desfaz nem interrompe os outros (ele continua ATIVO e o próximo dia tenta de novo).
 */
@Slf4j
@Component
public class CancelamentoJob {

    private final CancelamentoService service;

    public CancelamentoJob(CancelamentoService service) {
        this.service = service;
    }

    @Scheduled(cron = "${smartvan.agendador.cancelamentos.cron}", zone = "America/Sao_Paulo")
    public void executar() {
        encerrarVencidos();
    }

    /** Encerra cada contrato ATIVO com encerramento até hoje. Devolve quantos encerrou. */
    public int encerrarVencidos() {
        int encerrados = 0;
        for (Long contratoId : service.idsVencidos()) {
            try {
                if (service.encerrar(contratoId)) {
                    encerrados++;
                }
            } catch (RuntimeException e) {
                log.error("Falha ao encerrar o contrato {}", contratoId, e);
            }
        }
        return encerrados;
    }
}
