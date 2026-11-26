package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Visão somente de leitura da tabela {@code profissional}: só a tarifa de um profissional ativo conta como preço. */
@Entity
@Immutable
@Table(name = "profissional")
class ProfissionalDaBuscaJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private boolean ativo;

	protected ProfissionalDaBuscaJpaEntity() {
	}

}
