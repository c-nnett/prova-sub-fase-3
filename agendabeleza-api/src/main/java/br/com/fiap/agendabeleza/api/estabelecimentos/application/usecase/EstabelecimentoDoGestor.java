package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Carrega um estabelecimento e garante que o usuário é o gestor dele. Usado pelos casos de uso de alteração.
 */
final class EstabelecimentoDoGestor {

	private EstabelecimentoDoGestor() {
	}

	static Estabelecimento obter(EstabelecimentoRepository repositorio, UUID estabelecimentoId, UUID usuarioId) {
		Estabelecimento estabelecimento = repositorio.buscarPorId(estabelecimentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + estabelecimentoId + " não encontrado"));
		estabelecimento.garantirGestor(usuarioId);
		return estabelecimento;
	}

}
