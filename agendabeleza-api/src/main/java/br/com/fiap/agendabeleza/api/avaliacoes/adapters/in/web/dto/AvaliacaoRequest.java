package br.com.fiap.agendabeleza.api.avaliacoes.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequest(
		@NotNull @Min(1) @Max(5)
		@Schema(description = "Nota para o estabelecimento, de 1 a 5", example = "5")
		Integer notaEstabelecimento,

		@NotNull @Min(1) @Max(5)
		@Schema(description = "Nota para o profissional, de 1 a 5", example = "4")
		Integer notaProfissional,

		@Size(max = 1000)
		@Schema(description = "Comentário opcional, de até 1000 caracteres", example = "Atendimento pontual e caprichado")
		String comentario) {
}
