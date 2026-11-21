package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

public record EstabelecimentoResponse(
		UUID id,
		UUID gestorId,
		String nome,
		String descricao,
		EnderecoDto endereco,
		String timezone,
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
				estabelecimento.ativo(),
				estabelecimento.horarios().stream().map(HorarioDto::de).toList(),
				estabelecimento.fotos().stream().map(FotoResponse::de).toList());
	}

}
