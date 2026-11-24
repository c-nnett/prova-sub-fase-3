package br.com.fiap.agendabeleza.api.profissionais.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * Consulta pública do perfil de um profissional.
 */
public class ConsultarProfissionalUseCase {

	private final ProfissionalRepository profissionais;

	public ConsultarProfissionalUseCase(ProfissionalRepository profissionais) {
		this.profissionais = profissionais;
	}

	public Profissional executar(UUID profissionalId) {
		return ProfissionalEncontrado.obter(profissionais, profissionalId);
	}

}
