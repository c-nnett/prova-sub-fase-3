package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Substitui todos os horários de funcionamento. Dia ausente significa fechado.
 */
public record HorariosRequest(
		@NotNull @Schema(description = "Um item por dia da semana") List<@Valid @NotNull HorarioDto> horarios) {
}
