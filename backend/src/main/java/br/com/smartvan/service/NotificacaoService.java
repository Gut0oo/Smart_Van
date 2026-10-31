package br.com.smartvan.service;

import br.com.smartvan.dto.NotificacaoResponseDTO;
import br.com.smartvan.enums.TipoNotificacao;
import br.com.smartvan.exception.RegraNegocioException;
import br.com.smartvan.model.Notificacao;
import br.com.smartvan.model.Usuario;
import br.com.smartvan.repository.NotificacaoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/** Notificações dentro do app. Todos os módulos criam pelo {@link #notificar}. */
@Service
public class NotificacaoService {

    private final NotificacaoRepository repository;
    private final EntityManager entityManager;
    private final Clock clock;

    public NotificacaoService(NotificacaoRepository repository, EntityManager entityManager, Clock clock) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public void notificar(Long usuarioId, TipoNotificacao tipo, String titulo, String mensagem, Long referenciaId) {
        Notificacao n = new Notificacao();
        //referência sem SELECT: só precisa do id para a chave estrangeira
        n.setUsuario(entityManager.getReference(Usuario.class, usuarioId));
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setReferenciaId(referenciaId);
        n.setLida(false);
        n.setDataCriacao(clock.instant());
        repository.save(n);
    }

    /** {@code lidas == null} devolve todas; senão filtra. Mais recentes primeiro. */
    @Transactional(readOnly = true)
    public List<NotificacaoResponseDTO> listar(Long usuarioId, Boolean lidas) {
        List<Notificacao> lista = lidas == null
                ? repository.findByUsuarioIdOrderByDataCriacaoDescIdDesc(usuarioId)
                : repository.findByUsuarioIdAndLidaOrderByDataCriacaoDescIdDesc(usuarioId, lidas);
        return lista.stream().map(NotificacaoResponseDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long usuarioId) {
        return repository.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    /** Só o dono marca: id inexistente = 404 NAO_ENCONTRADO; de outro usuário = 403 SEM_PERMISSAO. */
    @Transactional
    public void marcarComoLida(Long usuarioId, Long notificacaoId) {
        Notificacao n = repository.findById(notificacaoId)
                .orElseThrow(() -> RegraNegocioException.naoEncontrado("Notificação"));
        if (!n.getUsuario().getId().equals(usuarioId)) {
            throw RegraNegocioException.semPermissao();
        }
        n.setLida(true);
    }
}
