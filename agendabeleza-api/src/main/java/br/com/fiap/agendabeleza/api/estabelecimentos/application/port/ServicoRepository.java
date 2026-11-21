package br.com.fiap.agendabeleza.api.estabelecimentos.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;

/**
 * Porta de persistência dos serviços oferecidos pelos estabelecimentos.
 */
public interface ServicoRepository {

	Servico salvar(Servico servico);

	Optional<Servico> buscarPorId(UUID id);

	List<Servico> listarPorEstabelecimento(UUID estabelecimentoId);

}
