package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.profissionais.domain.Especialidade;

public record EspecialidadeResponse(UUID id, String nome) {

	public static EspecialidadeResponse de(Especialidade especialidade) {
		return new EspecialidadeResponse(especialidade.id(), especialidade.nome());
	}

}
