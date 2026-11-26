package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.util.UUID;

import org.hibernate.annotations.Immutable;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Visão somente de leitura da tabela {@code servico}, usada nos filtros de serviço, categoria e preço. */
@Entity
@Immutable
@Table(name = "servico")
class ServicoDaBuscaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "estabelecimento_id", nullable = false)
	private UUID estabelecimentoId;

	@Column(nullable = false)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CategoriaServico categoria;

	@Column(nullable = false)
	private boolean ativo;

	protected ServicoDaBuscaJpaEntity() {
	}

}
