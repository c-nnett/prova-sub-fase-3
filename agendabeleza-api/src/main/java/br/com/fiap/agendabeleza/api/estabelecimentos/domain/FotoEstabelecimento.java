package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import java.util.UUID;

/**
 * Foto do estabelecimento, guardada como URL pública (planejamento, seção 2.1). A ordem começa em 1.
 */
public record FotoEstabelecimento(UUID id, String url, int ordem) {

	FotoEstabelecimento comOrdem(int novaOrdem) {
		return new FotoEstabelecimento(id, url, novaOrdem);
	}

}
