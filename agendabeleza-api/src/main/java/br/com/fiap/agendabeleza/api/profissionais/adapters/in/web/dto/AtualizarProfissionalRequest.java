package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarProfissionalRequest(
		@NotBlank @Size(max = 120) @Schema(example = "Ana Souza") String nome,
		@NotBlank @Email @Size(max = 180) @Schema(example = "ana@salao.com") String email,
		@Size(max = 2000) String bio,
		@NotNull @Schema(description = "false retira o profissional da listagem pública") Boolean ativo) {
}
