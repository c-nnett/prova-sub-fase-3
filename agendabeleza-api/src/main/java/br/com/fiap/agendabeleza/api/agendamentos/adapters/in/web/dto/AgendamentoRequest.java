package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgendamentoRequest(
		@NotNull UUID profissionalId,
		@NotNull UUID servicoId,
		@NotNull
		@Schema(description = "Início do atendimento em ISO 8601, com fuso. O fim é calculado pela duração do serviço",
				example = "2030-03-04T10:00:00-03:00")
		Instant inicio,
		@NotBlank @Email @Size(max = 180)
		@Schema(description = "E-mail do cliente para a confirmação, o cancelamento e o reagendamento",
				example = "cliente@email.com")
		String emailCliente,
		@Size(max = 500) @Schema(example = "Prefiro tesoura") String observacoes) {
}
