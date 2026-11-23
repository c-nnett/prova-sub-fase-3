package br.com.fiap.agendabeleza.api.agendamentos.adapters.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agendamento")
@Getter
@Setter
@NoArgsConstructor
class AgendamentoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "cliente_id", nullable = false, updatable = false)
	private UUID clienteId;

	@Column(name = "estabelecimento_id", nullable = false, updatable = false)
	private UUID estabelecimentoId;

	@Column(name = "profissional_id", nullable = false, updatable = false)
	private UUID profissionalId;

	@Column(name = "servico_id", nullable = false, updatable = false)
	private UUID servicoId;

	@Column(nullable = false)
	private Instant inicio;

	@Column(nullable = false)
	private Instant fim;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusAgendamento status;

	@Column(nullable = false, precision = 10, scale = 2, updatable = false)
	private BigDecimal preco;

	@Column(length = 500, updatable = false)
	private String observacoes;

	/** Bloqueio otimista. Nula no agendamento novo, o que faz o Spring Data usar {@code persist}. */
	@Version
	@Column(nullable = false)
	private Long versao;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@UpdateTimestamp
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

}
