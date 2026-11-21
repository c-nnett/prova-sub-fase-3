package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Endereco;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Atualiza os dados cadastrais de um estabelecimento. Somente o gestor dono pode alterar.
 */
public class AtualizarEstabelecimentoUseCase {

	private final EstabelecimentoRepository repositorio;

	public AtualizarEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		this.repositorio = repositorio;
	}

	public Estabelecimento executar(Comando comando) {
		Estabelecimento estabelecimento = EstabelecimentoDoGestor.obter(
				repositorio, comando.estabelecimentoId(), comando.usuarioId());
		estabelecimento.atualizarDados(comando.nome(), comando.descricao(), comando.endereco(), comando.timezone());
		return repositorio.salvar(estabelecimento);
	}

	public record Comando(
			UUID estabelecimentoId,
			UUID usuarioId,
			String nome,
			String descricao,
			Endereco endereco,
			String timezone) {
	}

}
