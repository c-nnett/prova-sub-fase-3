package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirServicosProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.domain.ServicoProfissional;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record ServicoProfissionalDto(
		@NotNull UUID servicoId,
		@DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
		@Schema(example = "95.00", description = "Na requisição, ausente assume o preço base do serviço")
		BigDecimal tarifa) {

	public DefinirServicosProfissionalUseCase.Item paraItem() {
		return new DefinirServicosProfissionalUseCase.Item(servicoId, tarifa);
	}

	public static ServicoProfissionalDto de(ServicoProfissional servico) {
		return new ServicoProfissionalDto(servico.servicoId(), servico.tarifa());
	}

}
