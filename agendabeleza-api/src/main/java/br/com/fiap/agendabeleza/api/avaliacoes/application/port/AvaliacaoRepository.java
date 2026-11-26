package br.com.fiap.agendabeleza.api.avaliacoes.application.port;

import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;

/**
 * Porta de persistência das avaliações. A restrição única em {@code agendamento_id} garante uma avaliação por
 * atendimento.
 */
public interface AvaliacaoRepository {

	Avaliacao salvar(Avaliacao avaliacao);

	boolean existeDoAgendamento(UUID agendamentoId);

	/** Da mais recente para a mais antiga. */
	List<Avaliacao> listarDoEstabelecimento(UUID estabelecimentoId);

	/** Da mais recente para a mais antiga. */
	List<Avaliacao> listarDoProfissional(UUID profissionalId);

	/** Média das notas dadas ao estabelecimento em todas as avaliações gravadas. */
	NotaMedia notasDoEstabelecimento(UUID estabelecimentoId);

	/** Média das notas dadas ao profissional em todas as avaliações gravadas. */
	NotaMedia notasDoProfissional(UUID profissionalId);

}
