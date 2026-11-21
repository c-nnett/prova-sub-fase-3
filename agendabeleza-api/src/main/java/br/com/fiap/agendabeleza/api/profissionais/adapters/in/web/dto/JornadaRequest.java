package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.util.List;

import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Substitui a jornada semanal. Aceita mais de um período por dia, como manhã e tarde. Lista vazia remove todos.
 */
public record JornadaRequest(
		@NotNull @Schema(description = "Cada período precisa caber no funcionamento do estabelecimento naquele dia")
		List<@Valid @NotNull PeriodoDto> periodos) {

	public List<PeriodoTrabalho> paraDominio() {
		return periodos.stream().map(PeriodoDto::paraDominio).toList();
	}

}
