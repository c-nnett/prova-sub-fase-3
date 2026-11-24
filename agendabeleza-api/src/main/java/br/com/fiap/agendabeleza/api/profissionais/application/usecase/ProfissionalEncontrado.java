package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Busca um profissional pelo id ou falha com {@link RecursoNaoEncontradoException}.
 */
final class ProfissionalEncontrado {

	private ProfissionalEncontrado() {
	}

	static Profissional obter(ProfissionalRepository profissionais, UUID profissionalId) {
		return profissionais.buscarPorId(profissionalId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Profissional " + profissionalId + " não encontrado"));
	}

}
