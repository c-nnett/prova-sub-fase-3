package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Visão somente de leitura da tabela {@code profissional_servico}: a tarifa que cada profissional cobra por um
 * serviço, que é o preço usado no filtro de faixa de preço.
 */
@Entity
@Immutable
@Table(name = "profissional_servico")
@IdClass(TarifaDaBuscaJpaEntity.Chave.class)
class TarifaDaBuscaJpaEntity {

	@Id
	@Column(name = "profissional_id")
	private UUID profissionalId;

	@Id
	@Column(name = "servico_id")
	private UUID servicoId;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal tarifa;

	protected TarifaDaBuscaJpaEntity() {
	}

	record Chave(UUID profissionalId, UUID servicoId) implements Serializable {
	}

}
