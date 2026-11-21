package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfissionalRequest(
		@Schema(description = "Login do profissional, quando ele tiver um. Permite que ele mesmo altere o perfil")
		UUID usuarioId,
		@NotBlank @Size(max = 120) @Schema(example = "Ana Souza") String nome,
		@NotBlank @Email @Size(max = 180) @Schema(example = "ana@salao.com") String email,
		@Size(max = 2000) String bio) {
}
