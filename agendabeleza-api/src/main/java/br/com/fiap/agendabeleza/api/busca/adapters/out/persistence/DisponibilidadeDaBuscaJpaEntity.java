package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Visão somente de leitura da tabela {@code disponibilidade_profissional}: a jornada semanal dos profissionais, usada
 * no filtro de disponibilidade.
 */
@Entity
@Immutable
@Table(name = "disponibilidade_profissional")
class DisponibilidadeDaBuscaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "profissional_id", nullable = false)
	private UUID profissionalId;

	/** 1 (segunda) a 7 (domingo), ISO-8601. */
	@Column(name = "dia_semana", nullable = false)
	private short diaSemana;

	@Column(name = "hora_inicio", nullable = false)
	private LocalTime horaInicio;

	@Column(name = "hora_fim", nullable = false)
	private LocalTime horaFim;

	protected DisponibilidadeDaBuscaJpaEntity() {
	}

}
