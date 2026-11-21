package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Atualização dos dados cadastrais. Horários e fotos têm rotas próprias.
 */
public record DadosEstabelecimentoRequest(
		@NotBlank @Size(max = 150) String nome,
		String descricao,
		@NotNull @Valid EnderecoDto endereco,
		@Schema(example = "America/Sao_Paulo", description = "Padrão: America/Sao_Paulo") String timezone) {
}
