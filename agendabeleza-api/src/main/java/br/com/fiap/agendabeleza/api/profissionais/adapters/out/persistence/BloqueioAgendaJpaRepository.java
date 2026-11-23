package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface BloqueioAgendaJpaRepository extends JpaRepository<BloqueioAgendaJpaEntity, UUID> {

	/** Sobreposição de intervalos semiabertos: {@code inicio < fimPeriodo AND fim > inicioPeriodo}. */
	List<BloqueioAgendaJpaEntity> findByProfissionalIdAndInicioBeforeAndFimAfterOrderByInicio(UUID profissionalId,
			Instant fimPeriodo, Instant inicioPeriodo);

	List<BloqueioAgendaJpaEntity> findByProfissionalIdAndFimAfterOrderByInicio(UUID profissionalId, Instant agora);

}
