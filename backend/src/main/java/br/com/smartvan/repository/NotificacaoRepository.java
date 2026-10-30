package br.com.smartvan.repository;

import br.com.smartvan.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    //mais recentes primeiro; o id desempata notificações criadas no mesmo instante
    List<Notificacao> findByUsuarioIdOrderByDataCriacaoDescIdDesc(Long usuarioId);

    List<Notificacao> findByUsuarioIdAndLidaOrderByDataCriacaoDescIdDesc(Long usuarioId, boolean lida);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}
