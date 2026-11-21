package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

/**
 * Lista pública dos serviços ativos de um estabelecimento, em ordem de nome.
 */
public class ListarServicosUseCase {

	private final ConsultarEstabelecimentoUseCase consultarEstabelecimento;
	private final ServicoRepository servicos;

	public ListarServicosUseCase(EstabelecimentoRepository estabelecimentos, ServicoRepository servicos) {
		this.consultarEstabelecimento = new ConsultarEstabelecimentoUseCase(estabelecimentos);
		this.servicos = servicos;
	}

	public List<Servico> executar(UUID estabelecimentoId) {
		consultarEstabelecimento.executar(estabelecimentoId);
		return servicos.listarPorEstabelecimento(estabelecimentoId).stream()
				.filter(Servico::ativo)
				.toList();
	}

}
