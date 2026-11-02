package br.com.smartvan.service;

import br.com.smartvan.enums.StatusMensalidade;
import br.com.smartvan.repository.MensalidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta exposta a outros módulos (cancelamento). */
@Service
public class MensalidadeConsultaService {

    private final MensalidadeRepository mensalidades;

    public MensalidadeConsultaService(MensalidadeRepository mensalidades) {
        this.mensalidades = mensalidades;
    }

    @Transactional(readOnly = true)
    public boolean existePendenteOuAtrasada(Long contratoId) {
        return mensalidades.existsByContratoIdAndStatusIn(contratoId,
                List.of(StatusMensalidade.PENDENTE, StatusMensalidade.ATRASADA));
    }
}
