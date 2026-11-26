package br.com.fiap.agendabeleza.api.estabelecimentos.application.port;

import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Porta de persistência do agregado {@link Estabelecimento}, incluindo fotos e horários.
 */
public interface EstabelecimentoRepository {

	/** Não altera a nota média nem o total de avaliações, que só mudam por {@link #atualizarAvaliacoes}. */
	Estabelecimento salvar(Estabelecimento estabelecimento);

	Optional<Estabelecimento> buscarPorId(UUID id);

	/**
	 * Grava só a nota média e o total de avaliações. Separado do {@link #salvar} para que uma edição do gestor feita
	 * ao mesmo tempo não sobrescreva as notas recalculadas por uma avaliação.
	 */
	void atualizarAvaliacoes(UUID estabelecimentoId, NotaMedia notas);

}
