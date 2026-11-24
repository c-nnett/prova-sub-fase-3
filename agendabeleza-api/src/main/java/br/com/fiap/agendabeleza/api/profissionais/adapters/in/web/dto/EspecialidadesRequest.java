package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Substitui as especialidades do profissional. Lista vazia remove todas.
 */
public record EspecialidadesRequest(
		@NotNull @Schema(description = "Ids do catálogo de GET /especialidades") List<@NotNull UUID> especialidadeIds) {
}
