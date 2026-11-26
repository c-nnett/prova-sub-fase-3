package br.com.fiap.agendabeleza.api.busca.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.busca.domain.EstabelecimentoEncontrado;

/** Resumo de um estabelecimento encontrado; os detalhes ficam em {@code GET /estabelecimentos/{id}}. */
public record EstabelecimentoEncontradoResponse(UUID id, String nome, String bairro, String cidade, String uf,
		BigDecimal notaMedia, int totalAvaliacoes) {

	public static EstabelecimentoEncontradoResponse de(EstabelecimentoEncontrado estabelecimento) {
		return new EstabelecimentoEncontradoResponse(estabelecimento.id(), estabelecimento.nome(),
				estabelecimento.bairro(), estabelecimento.cidade(), estabelecimento.uf(), estabelecimento.notaMedia(),
				estabelecimento.totalAvaliacoes());
	}

}
