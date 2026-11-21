package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Linha de {@code profissional_servico}: o serviço executado e a tarifa do profissional. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class ServicoProfissionalJpaEmbeddable {

	@Column(name = "servico_id", nullable = false)
	private UUID servicoId;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal tarifa;

}
