package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

public record ServicoResponse(
		UUID id,
		UUID estabelecimentoId,
		String nome,
		String descricao,
		CategoriaServico categoria,
		int duracaoMinutos,
		BigDecimal precoBase,
		boolean ativo) {

	public static ServicoResponse de(Servico servico) {
		return new ServicoResponse(servico.id(), servico.estabelecimentoId(), servico.nome(), servico.descricao(),
				servico.categoria(), servico.duracaoMinutos(), servico.precoBase(), servico.ativo());
	}

}
