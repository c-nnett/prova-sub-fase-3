package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Visão somente de leitura da tabela {@code profissional}: só a tarifa e a jornada de um profissional ativo contam na
 * busca.
 */
@Entity
@Immutable
@Table(name = "profissional")
class ProfissionalDaBuscaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "estabelecimento_id", nullable = false)
	private UUID estabelecimentoId;

	@Column(nullable = false)
	private boolean ativo;

	protected ProfissionalDaBuscaJpaEntity() {
	}

}
