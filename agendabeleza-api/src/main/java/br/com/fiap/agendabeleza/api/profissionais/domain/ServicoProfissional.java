package br.com.fiap.agendabeleza.api.profissionais.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Serviço que o profissional executa e a tarifa que ele cobra, guardada com duas casas decimais.
 */
public record ServicoProfissional(UUID servicoId, BigDecimal tarifa) {

	public ServicoProfissional {
		obrigatorio(servicoId, "servicoId");
		obrigatorio(tarifa, "tarifa");
		if (tarifa.signum() < 0) {
			throw new RegraNegocioException("tarifa não pode ser negativa");
		}
		if (tarifa.stripTrailingZeros().scale() > 2) {
			throw new RegraNegocioException("tarifa deve ter no máximo duas casas decimais");
		}
		tarifa = tarifa.setScale(2, RoundingMode.UNNECESSARY);
	}

}
