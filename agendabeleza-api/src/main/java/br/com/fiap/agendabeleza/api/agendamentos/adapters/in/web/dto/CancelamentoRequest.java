package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Corpo opcional do cancelamento. */
public record CancelamentoRequest(
		@Size(max = 250) @Schema(description = "Registrado no histórico do agendamento", example = "Imprevisto")
		String motivo) {
}
