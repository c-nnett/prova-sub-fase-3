package br.com.fiap.agendabeleza.api.busca.adapters.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Visão somente de leitura da tabela {@code estabelecimento}, com as colunas que a busca filtra e devolve. O módulo de
 * estabelecimentos continua dono da tabela; a busca não pode usar o adaptador dele (arquitetura, seção 4.3).
 */
@Entity
@Immutable
@Table(name = "estabelecimento")
class EstabelecimentoDaBuscaJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false)
	private String bairro;

	@Column(nullable = false)
	private String cidade;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(nullable = false, length = 2)
	private String uf;

	@Column(name = "nota_media", precision = 3, scale = 2)
	private BigDecimal notaMedia;

	@Column(name = "total_avaliacoes", nullable = false)
	private int totalAvaliacoes;

	@Column(nullable = false)
	private boolean ativo;

	protected EstabelecimentoDaBuscaJpaEntity() {
	}

	EstabelecimentoEncontrado paraDominio() {
		return new EstabelecimentoEncontrado(id, nome, bairro, cidade, uf, notaMedia, totalAvaliacoes);
	}

}
