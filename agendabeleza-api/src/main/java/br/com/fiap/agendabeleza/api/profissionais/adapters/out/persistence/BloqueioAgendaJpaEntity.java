package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bloqueio de agenda. Fica fora do grafo de {@link ProfissionalJpaEntity}, porque é um agregado próprio.
 */
@Entity
@Table(name = "bloqueio_agenda")
@Getter
@Setter
@NoArgsConstructor
class BloqueioAgendaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "profissional_id", nullable = false)
	private UUID profissionalId;

	@Column(nullable = false)
	private Instant inicio;

	@Column(nullable = false)
	private Instant fim;

	@Column(length = 150)
	private String motivo;

}
