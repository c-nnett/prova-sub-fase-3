package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FotoRequest(
		@NotBlank @Size(max = 500) @Schema(example = "https://cdn.exemplo.com/fachada.jpg") String url) {
}
