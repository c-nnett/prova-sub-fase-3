package br.com.fiap.agendabeleza.api.profissionais.adapters.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.fiap.agendabeleza.api.profissionais.application.port.EspecialidadeRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;

/**
 * Implementa {@link EspecialidadeRepository} com JPA. O catálogo é carregado pela migração e só é lido.
 */
@Component
class EspecialidadeJpaAdapter implements EspecialidadeRepository {

	private final EspecialidadeJpaRepository repositorio;

	EspecialidadeJpaAdapter(EspecialidadeJpaRepository repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Especialidade> buscarPorIds(Collection<UUID> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		return repositorio.findAllById(ids).stream().map(EspecialidadeJpaAdapter::paraDominio).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Especialidade> listarTodas() {
		return repositorio.findAll().stream().map(EspecialidadeJpaAdapter::paraDominio).toList();
	}

	private static Especialidade paraDominio(EspecialidadeJpaEntity entidade) {
		return Especialidade.reconstituir(entidade.getId(), entidade.getNome());
	}

}
