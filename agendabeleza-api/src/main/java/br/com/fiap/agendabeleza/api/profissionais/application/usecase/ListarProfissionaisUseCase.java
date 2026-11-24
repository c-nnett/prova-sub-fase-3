package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Lista os profissionais ativos de um estabelecimento. Consulta pública.
 */
public class ListarProfissionaisUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;

	public ListarProfissionaisUseCase(EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais) {
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
	}

	public List<Profissional> executar(UUID estabelecimentoId) {
		if (estabelecimentos.buscarPorId(estabelecimentoId).isEmpty()) {
			throw new RecursoNaoEncontradoException("Estabelecimento " + estabelecimentoId + " não encontrado");
		}
		return profissionais.listarPorEstabelecimento(estabelecimentoId).stream()
				.filter(Profissional::ativo)
				.toList();
	}

}
