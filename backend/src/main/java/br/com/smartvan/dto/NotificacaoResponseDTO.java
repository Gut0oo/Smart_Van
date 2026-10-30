package br.com.smartvan.dto;

import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.model.Notificacao;

import java.time.Instant;

public record NotificacaoResponseDTO(Long id, TipoNotificacao tipo, String titulo, String mensagem,
                                     Long referenciaId, boolean lida, Instant dataCriacao) {

    public static NotificacaoResponseDTO de(Notificacao n) {
        return new NotificacaoResponseDTO(n.getId(), n.getTipo(), n.getTitulo(), n.getMensagem(),
                n.getReferenciaId(), n.isLida(), n.getDataCriacao());
    }
}
