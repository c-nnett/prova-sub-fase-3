package br.com.fiap.agendabeleza.api.agendamentos.adapters.out.persistence;

import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Linha da trilha de alterações. Somente inserção: nenhuma coluna é atualizável. */
@Entity
@Table(name = "agendamento_historico")
@Getter
@Setter
@NoArgsConstructor
class AgendamentoHistoricoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "agendamento_id", nullable = false, updatable = false)
	private UUID agendamentoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_anterior", length = 20, updatable = false)
	private StatusAgendamento statusAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "status_novo", nullable = false, length = 20, updatable = false)
	private StatusAgendamento statusNovo;

	@Column(name = "inicio_anterior", updatable = false)
	private Instant inicioAnterior;

	@Column(name = "inicio_novo", updatable = false)
	private Instant inicioNovo;

	@Column(length = 250, updatable = false)
	private String motivo;

	@Column(name = "alterado_por", nullable = false, updatable = false)
	private UUID alteradoPor;

	@Column(name = "ocorrido_em", nullable = false, updatable = false)
	private Instant ocorridoEm;

}
