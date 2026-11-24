package br.com.fiap.agendabeleza.api.profissionais.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Porta de persistência dos profissionais, com especialidades, serviços e jornada.
 */
public interface ProfissionalRepository {

	Profissional salvar(Profissional profissional);

	Optional<Profissional> buscarPorId(UUID id);

	List<Profissional> listarPorEstabelecimento(UUID estabelecimentoId);

}
