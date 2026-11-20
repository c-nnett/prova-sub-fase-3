package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Remove uma foto do estabelecimento e reordena as restantes.
 */
public class RemoverFotoEstabelecimentoUseCase {

	private final EstabelecimentoRepository repositorio;

	public RemoverFotoEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		this.repositorio = repositorio;
	}

	public void executar(Comando comando) {
		Estabelecimento estabelecimento = EstabelecimentoDoGestor.obter(
				repositorio, comando.estabelecimentoId(), comando.usuarioId());
		estabelecimento.removerFoto(comando.fotoId());
		repositorio.salvar(estabelecimento);
	}

	public record Comando(UUID estabelecimentoId, UUID usuarioId, UUID fotoId) {
	}

}
