package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Visão somente de leitura da tabela {@code horario_funcionamento}, usada no filtro de disponibilidade. */
@Entity
@Immutable
@Table(name = "horario_funcionamento")
class HorarioFuncionamentoDaBuscaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "estabelecimento_id", nullable = false)
	private UUID estabelecimentoId;

	/** 1 (segunda) a 7 (domingo), ISO-8601. */
	@Column(name = "dia_semana", nullable = false)
	private short diaSemana;

	@Column(name = "abre_as", nullable = false)
	private LocalTime abreAs;

	@Column(name = "fecha_as", nullable = false)
	private LocalTime fechaAs;

	protected HorarioFuncionamentoDaBuscaJpaEntity() {
	}

}
