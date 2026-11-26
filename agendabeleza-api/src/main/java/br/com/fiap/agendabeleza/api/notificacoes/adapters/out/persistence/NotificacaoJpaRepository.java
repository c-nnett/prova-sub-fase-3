package br.com.fiap.agendabeleza.api.notificacoes.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.StatusNotificacao;

interface NotificacaoJpaRepository extends JpaRepository<NotificacaoJpaEntity, UUID> {

	/** Atendida pelo índice parcial {@code idx_notificacao_pendente}. */
	List<NotificacaoJpaEntity> findByStatusAndEnviarEmLessThanEqualOrderByEnviarEm(StatusNotificacao status,
			Instant momento, Limit limite);

	Optional<NotificacaoJpaEntity> findFirstByAgendamentoIdAndPapelOrderByEnviarEm(UUID agendamentoId,
			PapelDestinatario papel);

}
