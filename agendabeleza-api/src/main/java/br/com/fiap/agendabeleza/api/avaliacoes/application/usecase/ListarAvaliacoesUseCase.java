package br.com.fiap.agendabeleza.api.avaliacoes.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import br.com.fiap.agendabeleza.api.avaliacoes.application.port.AvaliacaoRepository;
import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Listagem pública das avaliações de um estabelecimento ou de um profissional, da mais recente para a mais antiga.
 */
public class ListarAvaliacoesUseCase {

	private final AvaliacaoRepository avaliacoes;
	private final EstabelecimentoRepository estabelecimentos;
	private final ProfissionalRepository profissionais;

	public ListarAvaliacoesUseCase(AvaliacaoRepository avaliacoes, EstabelecimentoRepository estabelecimentos,
			ProfissionalRepository profissionais) {
		this.avaliacoes = avaliacoes;
		this.estabelecimentos = estabelecimentos;
		this.profissionais = profissionais;
	}

	/** @throws RecursoNaoEncontradoException se o estabelecimento não existir */
	public List<Avaliacao> doEstabelecimento(UUID estabelecimentoId) {
		return listarSeExistir("Estabelecimento", estabelecimentoId, estabelecimentos::buscarPorId,
				avaliacoes::listarDoEstabelecimento);
	}

	/** @throws RecursoNaoEncontradoException se o profissional não existir */
	public List<Avaliacao> doProfissional(UUID profissionalId) {
		return listarSeExistir("Profissional", profissionalId, profissionais::buscarPorId,
				avaliacoes::listarDoProfissional);
	}

	private static List<Avaliacao> listarSeExistir(String recurso, UUID id, Function<UUID, Optional<?>> buscar,
			Function<UUID, List<Avaliacao>> listar) {
		if (buscar.apply(id).isEmpty()) {
			throw new RecursoNaoEncontradoException(recurso + " " + id + " não encontrado");
		}
		return listar.apply(id);
	}

}
