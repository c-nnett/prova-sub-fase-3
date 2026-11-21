package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.com.fiap.agendabeleza.api.profissionais.application.port.EspecialidadeRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Resolve os nomes das especialidades para montar o perfil. Consulta o catálogo uma única vez, mesmo para
 * vários profissionais. Especialidade que não está mais no catálogo é ignorada.
 */
public class DetalharProfissionaisUseCase {

	private final EspecialidadeRepository especialidades;

	public DetalharProfissionaisUseCase(EspecialidadeRepository especialidades) {
		this.especialidades = especialidades;
	}

	public PerfilProfissional executar(Profissional profissional) {
		return executar(List.of(profissional)).get(0);
	}

	public List<PerfilProfissional> executar(List<Profissional> profissionais) {
		Set<UUID> ids = new LinkedHashSet<>();
		profissionais.forEach(profissional -> ids.addAll(profissional.especialidadeIds()));
		Map<UUID, Especialidade> catalogo = ids.isEmpty() ? Map.of() : buscar(ids);
		return profissionais.stream()
				.map(profissional -> new PerfilProfissional(profissional, profissional.especialidadeIds().stream()
						.map(catalogo::get)
						.filter(Objects::nonNull)
						.toList()))
				.toList();
	}

	private Map<UUID, Especialidade> buscar(Collection<UUID> ids) {
		return especialidades.buscarPorIds(ids).stream()
				.collect(Collectors.toMap(Especialidade::id, Function.identity()));
	}

}
