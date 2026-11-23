package br.com.fiap.agendabeleza.api.agendamentos.adapters.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;

interface AgendamentoJpaRepository extends JpaRepository<AgendamentoJpaEntity, UUID> {

	/** Mesma semântica de {@code [)} da restrição: intervalos que só se encostam não se cruzam. */
	@Query("""
			select a from AgendamentoJpaEntity a
			where a.profissionalId = :profissionalId
			  and a.status in :status
			  and a.inicio < :ate
			  and a.fim > :de
			order by a.inicio
			""")
	List<AgendamentoJpaEntity> buscarQueCruzam(@Param("profissionalId") UUID profissionalId,
			@Param("status") Collection<StatusAgendamento> status, @Param("de") Instant de, @Param("ate") Instant ate);

	List<AgendamentoJpaEntity> findByClienteIdOrderByInicioDesc(UUID clienteId);

}
