package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

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
@Table(name = "horario_funcionamento")
@Getter
@Setter
@NoArgsConstructor
class HorarioFuncionamentoJpaEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estabelecimento_id", nullable = false)
	private EstabelecimentoJpaEntity estabelecimento;

	/** 1 (segunda) a 7 (domingo), ISO-8601, igual a {@link java.time.DayOfWeek#getValue()}. */
	@Column(name = "dia_semana", nullable = false)
	private short diaSemana;

	@Column(name = "abre_as", nullable = false)
	private LocalTime abreAs;

	@Column(name = "fecha_as", nullable = false)
	private LocalTime fechaAs;

}
