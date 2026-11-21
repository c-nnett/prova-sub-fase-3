package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import java.math.BigDecimal;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ServicoRequest(
		@NotBlank @Size(max = 120) String nome,
		String descricao,
		@NotNull CategoriaServico categoria,
		@NotNull @Positive @Schema(example = "60") Integer duracaoMinutos,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) @Schema(example = "80.00") BigDecimal precoBase) {
}
