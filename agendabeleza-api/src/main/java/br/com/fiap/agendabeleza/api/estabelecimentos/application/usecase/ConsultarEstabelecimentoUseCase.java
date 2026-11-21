package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Consulta pública dos detalhes de um estabelecimento, com fotos e horários.
 */
public class ConsultarEstabelecimentoUseCase {

	private final EstabelecimentoRepository repositorio;

	public ConsultarEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		this.repositorio = repositorio;
	}

	public Estabelecimento executar(UUID estabelecimentoId) {
		return repositorio.buscarPorId(estabelecimentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + estabelecimentoId + " não encontrado"));
	}

}
