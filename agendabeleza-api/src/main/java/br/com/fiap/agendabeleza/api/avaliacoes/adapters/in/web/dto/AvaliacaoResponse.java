package br.com.fiap.agendabeleza.api.avaliacoes.adapters.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import io.swagger.v3.oas.annotations.media.Schema;

/** Sem o identificador do cliente: as listas de avaliações são públicas. */
public record AvaliacaoResponse(
		UUID id,
		UUID agendamentoId,
		UUID estabelecimentoId,
		UUID profissionalId,
		int notaEstabelecimento,
		int notaProfissional,
		String comentario,
		@Schema(description = "Em UTC") Instant criadaEm) {

	public static AvaliacaoResponse de(Avaliacao avaliacao) {
		return new AvaliacaoResponse(avaliacao.id(), avaliacao.agendamentoId(), avaliacao.estabelecimentoId(),
				avaliacao.profissionalId(), avaliacao.notaEstabelecimento(), avaliacao.notaProfissional(),
				avaliacao.comentario(), avaliacao.criadaEm());
	}

}
