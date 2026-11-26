package br.com.fiap.agendabeleza.api.avaliacoes.adapters.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "avaliacao")
@Getter
@Setter
@NoArgsConstructor
class AvaliacaoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "agendamento_id", nullable = false, unique = true)
	private UUID agendamentoId;

	@Column(name = "estabelecimento_id", nullable = false)
	private UUID estabelecimentoId;

	@Column(name = "profissional_id", nullable = false)
	private UUID profissionalId;

	@Column(name = "cliente_id", nullable = false)
	private UUID clienteId;

	@Column(name = "nota_estabelecimento", nullable = false)
	private short notaEstabelecimento;

	@Column(name = "nota_profissional", nullable = false)
	private short notaProfissional;

	@Column(length = 1000)
	private String comentario;

	@Column(name = "criada_em", nullable = false)
	private Instant criadaEm;

}
