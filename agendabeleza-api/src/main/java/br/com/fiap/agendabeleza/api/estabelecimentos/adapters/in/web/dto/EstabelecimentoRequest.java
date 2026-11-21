package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de estabelecimento. Horários e fotos são opcionais e podem ser definidos depois.
 */
public record EstabelecimentoRequest(
		@NotBlank @Size(max = 150) String nome,
		String descricao,
		@NotNull @Valid EnderecoDto endereco,
		@Schema(example = "America/Sao_Paulo", description = "Padrão: America/Sao_Paulo") String timezone,
		List<@Valid HorarioDto> horarios,
		@Schema(description = "URLs públicas das fotos, na ordem de exibição") List<String> fotos) {
}
