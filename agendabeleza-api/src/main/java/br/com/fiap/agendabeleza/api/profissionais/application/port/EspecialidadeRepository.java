package br.com.fiap.agendabeleza.api.profissionais.application.port;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;

/**
 * Porta de leitura do catálogo de especialidades, carregado pela migração.
 */
public interface EspecialidadeRepository {

	/** Retorna as especialidades encontradas. Ids inexistentes ficam de fora. */
	List<Especialidade> buscarPorIds(Collection<UUID> ids);

	List<Especialidade> listarTodas();

}
