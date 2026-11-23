package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BloqueioRequest(
		@NotNull @Schema(description = "Início, com fuso", example = "2026-11-30T10:00:00-03:00")
		OffsetDateTime inicio,
		@NotNull @Schema(description = "Fim, exclusivo e com fuso", example = "2026-11-30T11:00:00-03:00")
		OffsetDateTime fim,
		@Size(max = 150) @Schema(example = "Consulta médica") String motivo) {
}
