package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.List;

import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Profissional com as especialidades do catálogo já resolvidas, ordenadas por nome.
 */
public record PerfilProfissional(Profissional profissional, List<Especialidade> especialidades) {

	public PerfilProfissional {
		especialidades = List.copyOf(especialidades);
	}

}
