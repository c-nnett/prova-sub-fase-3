package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "servico")
@Getter
@Setter
@NoArgsConstructor
class ServicoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "estabelecimento_id", nullable = false)
	private UUID estabelecimentoId;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(columnDefinition = "text")
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private CategoriaServico categoria;

	@Column(name = "duracao_minutos", nullable = false)
	private int duracaoMinutos;

	@Column(name = "preco_base", nullable = false, precision = 10, scale = 2)
	private BigDecimal precoBase;

	@Column(nullable = false)
	private boolean ativo;

	@CreationTimestamp
	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@UpdateTimestamp
	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

}
