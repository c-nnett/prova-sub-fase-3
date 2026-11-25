package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Carrega um estabelecimento e garante que o usuário é o gestor dele. Usado pelos casos de uso de alteração deste
 * módulo e pelo painel do módulo de agendamentos.
 */
public final class EstabelecimentoDoGestor {

	private EstabelecimentoDoGestor() {
	}

	static Estabelecimento obter(EstabelecimentoRepository repositorio, UUID estabelecimentoId, UUID usuarioId) {
		return obter(repositorio, estabelecimentoId, usuarioId, "alterá-lo");
	}

	/**
	 * @param acao o que só o gestor pode fazer, usado na mensagem do 403
	 * @throws RecursoNaoEncontradoException se o estabelecimento não existir
	 * @throws br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException se o usuário não for o gestor
	 */
	public static Estabelecimento obter(EstabelecimentoRepository repositorio, UUID estabelecimentoId,
			UUID usuarioId, String acao) {
		Estabelecimento estabelecimento = repositorio.buscarPorId(estabelecimentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + estabelecimentoId + " não encontrado"));
		estabelecimento.garantirGestor(usuarioId, acao);
		return estabelecimento;
	}

}
