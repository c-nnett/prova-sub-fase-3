package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto.AgendamentoResponse;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ComandoDeAlteracao;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConfirmarAgendamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Ações de gestão sobre um agendamento existente. Todas respondem 200 com o agendamento como ficou gravado, 404 se
 * ele não existir, 403 para quem não tem permissão, 422 para uma operação fora do diagrama de status e 409 se outra
 * operação tiver alterado o agendamento antes.
 */
@RestController
@RequestMapping("/api/v1/agendamentos/{id}")
@Tag(name = "Gestão de agendamentos", description = "Confirmar, cancelar, reagendar, concluir e registrar o não "
		+ "comparecimento")
class GestaoAgendamentoController {

	private static final String USUARIO = "X-Usuario-Id";

	private final ConfirmarAgendamentoUseCase confirmar;

	GestaoAgendamentoController(ConfirmarAgendamentoUseCase confirmar) {
		this.confirmar = confirmar;
	}

	@PostMapping("/confirmar")
	@Operation(summary = "Confirma o agendamento", description = "Somente o gestor do estabelecimento ou o "
			+ "profissional. Só um agendamento AGENDADO pode ser confirmado; os demais status respondem 422.")
	AgendamentoResponse confirmar(
			@PathVariable UUID id,
			@Parameter(description = "Gestor ou profissional que confirma") @RequestHeader(USUARIO) UUID usuarioId) {
		return AgendamentoResponse.de(confirmar.executar(new ComandoDeAlteracao.Simples(id, usuarioId)));
	}

}
