package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "disponibilidade_profissional")
@Getter
@Setter
@NoArgsConstructor
class DisponibilidadeJpaEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "profissional_id", nullable = false)
	private ProfissionalJpaEntity profissional;

	/** 1 (segunda) a 7 (domingo), ISO-8601, igual a {@link java.time.DayOfWeek#getValue()}. */
	@Column(name = "dia_semana", nullable = false)
	private short diaSemana;

	@Column(name = "hora_inicio", nullable = false)
	private LocalTime horaInicio;

	@Column(name = "hora_fim", nullable = false)
	private LocalTime horaFim;

}
