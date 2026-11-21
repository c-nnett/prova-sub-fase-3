package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.Comparator;
import java.util.List;

import br.com.fiap.agendabeleza.api.profissionais.application.port.EspecialidadeRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;

/**
 * Lista o catálogo de especialidades, ordenado por nome. Consulta pública.
 */
public class ListarEspecialidadesUseCase {

	private final EspecialidadeRepository especialidades;

	public ListarEspecialidadesUseCase(EspecialidadeRepository especialidades) {
		this.especialidades = especialidades;
	}

	public List<Especialidade> executar() {
		return especialidades.listarTodas().stream()
				.sorted(Comparator.comparing(Especialidade::nome, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

}
