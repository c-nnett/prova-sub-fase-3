package br.com.fiap.agendabeleza.api.avaliacoes.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import br.com.fiap.agendabeleza.api.avaliacoes.application.port.AvaliacaoRepository;
import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;

/**
 * Imita o adaptador JPA: recusa uma segunda avaliação do mesmo agendamento, como a restrição única, e calcula as
 * notas a partir da soma das avaliações gravadas.
 */
public class AvaliacaoRepositoryEmMemoria implements AvaliacaoRepository {

	private final List<Avaliacao> dados = new ArrayList<>();

	@Override
	public Avaliacao salvar(Avaliacao avaliacao) {
		if (existeDoAgendamento(avaliacao.agendamentoId())) {
			throw new IllegalStateException("Restrição única de agendamento_id violada");
		}
		dados.add(avaliacao);
		return avaliacao;
	}

	@Override
	public boolean existeDoAgendamento(UUID agendamentoId) {
		return dados.stream().anyMatch(avaliacao -> avaliacao.agendamentoId().equals(agendamentoId));
	}

	@Override
	public List<Avaliacao> listarDoEstabelecimento(UUID estabelecimentoId) {
		return maisRecentesPrimeiro(doEstabelecimento(estabelecimentoId));
	}

	@Override
	public List<Avaliacao> listarDoProfissional(UUID profissionalId) {
		return maisRecentesPrimeiro(doProfissional(profissionalId));
	}

	@Override
	public NotaMedia notasDoEstabelecimento(UUID estabelecimentoId) {
		return notas(doEstabelecimento(estabelecimentoId), Avaliacao::notaEstabelecimento);
	}

	@Override
	public NotaMedia notasDoProfissional(UUID profissionalId) {
		return notas(doProfissional(profissionalId), Avaliacao::notaProfissional);
	}

	public int total() {
		return dados.size();
	}

	private static Predicate<Avaliacao> doEstabelecimento(UUID estabelecimentoId) {
		return avaliacao -> avaliacao.estabelecimentoId().equals(estabelecimentoId);
	}

	private static Predicate<Avaliacao> doProfissional(UUID profissionalId) {
		return avaliacao -> avaliacao.profissionalId().equals(profissionalId);
	}

	private List<Avaliacao> maisRecentesPrimeiro(Predicate<Avaliacao> criterio) {
		return dados.stream()
				.filter(criterio)
				.sorted(Comparator.comparing(Avaliacao::criadaEm, Comparator.reverseOrder()))
				.toList();
	}

	private NotaMedia notas(Predicate<Avaliacao> criterio, ToIntFunction<Avaliacao> nota) {
		List<Avaliacao> selecionadas = dados.stream().filter(criterio).toList();
		long somaDasNotas = selecionadas.stream().mapToLong(nota::applyAsInt).sum();
		return NotaMedia.de(somaDasNotas, selecionadas.size());
	}

}
