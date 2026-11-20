package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "foto_estabelecimento")
@Getter
@Setter
@NoArgsConstructor
class FotoEstabelecimentoJpaEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estabelecimento_id", nullable = false)
	private EstabelecimentoJpaEntity estabelecimento;

	@Column(nullable = false, length = 500)
	private String url;

	@Column(nullable = false)
	private int ordem;

}
