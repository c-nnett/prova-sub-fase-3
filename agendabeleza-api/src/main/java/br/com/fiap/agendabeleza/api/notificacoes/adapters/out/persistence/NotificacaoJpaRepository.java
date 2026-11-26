package br.com.fiap.agendabeleza.api.notificacoes.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.StatusNotificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;

interface NotificacaoJpaRepository extends JpaRepository<NotificacaoJpaEntity, UUID> {

	/** Atendida pelo índice parcial {@code idx_notificacao_pendente}. */
	List<NotificacaoJpaEntity> findByStatusAndEnviarEmLessThanEqualOrderByEnviarEm(StatusNotificacao status,
			Instant momento, Limit limite);

	/** Atendida pelo índice {@code idx_notificacao_agendamento_papel}, que começa por {@code agendamento_id}. */
	List<NotificacaoJpaEntity> findByAgendamentoIdAndTipoAndStatus(UUID agendamentoId, TipoNotificacao tipo,
			StatusNotificacao status);

	Optional<NotificacaoJpaEntity> findFirstByAgendamentoIdAndPapelOrderByEnviarEm(UUID agendamentoId,
			PapelDestinatario papel);

}
