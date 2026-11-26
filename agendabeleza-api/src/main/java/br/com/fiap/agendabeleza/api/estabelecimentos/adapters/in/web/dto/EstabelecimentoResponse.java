package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import io.swagger.v3.oas.annotations.media.Schema;

public record EstabelecimentoResponse(
		UUID id,
		UUID gestorId,
		String nome,
		String descricao,
		EnderecoDto endereco,
		String timezone,
		@Schema(description = "Média das notas das avaliações, de 1 a 5. Nula até a primeira avaliação")
		BigDecimal notaMedia,
		int totalAvaliacoes,
		boolean ativo,
		List<HorarioDto> horarios,
		List<FotoResponse> fotos) {

	public static EstabelecimentoResponse de(Estabelecimento estabelecimento) {
		return new EstabelecimentoResponse(
				estabelecimento.id(),
				estabelecimento.gestorId(),
				estabelecimento.nome(),
				estabelecimento.descricao(),
				EnderecoDto.de(estabelecimento.endereco()),
				estabelecimento.timezone().getId(),
				estabelecimento.notaMedia(),
				estabelecimento.totalAvaliacoes(),
				estabelecimento.ativo(),
				estabelecimento.horarios().stream().map(HorarioDto::de).toList(),
				estabelecimento.fotos().stream().map(FotoResponse::de).toList());
	}

}
