package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.util.List;

import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirServicosProfissionalUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Substitui os serviços que o profissional executa e as tarifas. Lista vazia remove todos.
 */
public record ServicosProfissionalRequest(@NotNull List<@Valid @NotNull ServicoProfissionalDto> servicos) {

	public List<DefinirServicosProfissionalUseCase.Item> paraItens() {
		return servicos.stream().map(ServicoProfissionalDto::paraItem).toList();
	}

}
