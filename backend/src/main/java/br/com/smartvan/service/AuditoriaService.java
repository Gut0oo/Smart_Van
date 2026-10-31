package br.com.smartvan.service;

import br.com.smartvan.model.Auditoria;
import br.com.smartvan.repository.AuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Registra ações sensíveis (ex.: acao "BAIXA_MENSALIDADE", entidade "Mensalidade"). */
@Service
public class AuditoriaService {

    private final AuditoriaRepository repository;
    private final Clock clock;

    public AuditoriaService(AuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** {@code usuarioId} pode ser null quando a ação é do sistema (agendador). */
    @Transactional
    public void registrar(Long usuarioId, String acao, String entidade, Long entidadeId, String detalhes) {
        Auditoria a = new Auditoria();
        a.setUsuarioId(usuarioId);
        a.setAcao(acao);
        a.setEntidade(entidade);
        a.setEntidadeId(entidadeId);
        a.setDetalhes(detalhes);
        a.setDataRegistro(clock.instant());
        repository.save(a);
    }
}
