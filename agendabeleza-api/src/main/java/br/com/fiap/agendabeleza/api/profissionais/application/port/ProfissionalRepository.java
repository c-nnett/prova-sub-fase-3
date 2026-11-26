package br.com.fiap.agendabeleza.api.profissionais.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.NotaMedia;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Porta de persistência dos profissionais, com especialidades, serviços e jornada.
 */
public interface ProfissionalRepository {

	/** Não altera a nota média nem o total de avaliações, que só mudam por {@link #atualizarAvaliacoes}. */
	Profissional salvar(Profissional profissional);

	Optional<Profissional> buscarPorId(UUID id);

	List<Profissional> listarPorEstabelecimento(UUID estabelecimentoId);

	/**
	 * Grava só a nota média e o total de avaliações. Separado do {@link #salvar} para que uma edição do perfil feita
	 * ao mesmo tempo não sobrescreva as notas recalculadas por uma avaliação.
	 */
	void atualizarAvaliacoes(UUID profissionalId, NotaMedia notas);

}
