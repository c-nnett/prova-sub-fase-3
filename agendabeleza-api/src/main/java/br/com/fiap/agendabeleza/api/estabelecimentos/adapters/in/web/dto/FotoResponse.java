package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.FotoEstabelecimento;

public record FotoResponse(UUID id, String url, int ordem) {

	public static FotoResponse de(FotoEstabelecimento foto) {
		return new FotoResponse(foto.id(), foto.url(), foto.ordem());
	}

}
