package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ReagendamentoRequest(
		@NotNull
		@Schema(description = "Novo início em ISO 8601, com fuso. A duração do atendimento é mantida",
				example = "2030-03-04T14:00:00-03:00")
		Instant novoInicio) {
}
