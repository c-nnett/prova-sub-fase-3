package br.com.fiap.agendabeleza.api.agendamentos.adapters.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AgendamentoHistoricoJpaRepository extends JpaRepository<AgendamentoHistoricoJpaEntity, UUID> {

	List<AgendamentoHistoricoJpaEntity> findByAgendamentoIdOrderByOcorridoEm(UUID agendamentoId);

}
