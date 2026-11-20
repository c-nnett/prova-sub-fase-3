package br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.FotoEstabelecimento;

/**
 * Adiciona uma foto, informada como URL, ao fim da galeria do estabelecimento.
 */
public class AdicionarFotoEstabelecimentoUseCase {

	private final EstabelecimentoRepository repositorio;

	public AdicionarFotoEstabelecimentoUseCase(EstabelecimentoRepository repositorio) {
		this.repositorio = repositorio;
	}

	public FotoEstabelecimento executar(Comando comando) {
		Estabelecimento estabelecimento = EstabelecimentoDoGestor.obter(
				repositorio, comando.estabelecimentoId(), comando.usuarioId());
		FotoEstabelecimento foto = estabelecimento.adicionarFoto(comando.url());
		repositorio.salvar(estabelecimento);
		return foto;
	}

	public record Comando(UUID estabelecimentoId, UUID usuarioId, String url) {
	}

}
