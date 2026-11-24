package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto.AgendamentoResponse;
import br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto.CancelamentoRequest;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.AlteracaoDeAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CancelarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ComandoDeAlteracao;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConcluirAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConfirmarAgendamentoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

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
	private final CancelarAgendamentoUseCase cancelar;
	private final ConcluirAgendamentoUseCase concluir;

	GestaoAgendamentoController(ConfirmarAgendamentoUseCase confirmar, CancelarAgendamentoUseCase cancelar,
			ConcluirAgendamentoUseCase concluir) {
		this.confirmar = confirmar;
		this.cancelar = cancelar;
		this.concluir = concluir;
	}

	@PostMapping("/confirmar")
	@Operation(summary = "Confirma o agendamento", description = "Somente o gestor do estabelecimento ou o "
			+ "profissional. Só um agendamento AGENDADO pode ser confirmado; os demais status respondem 422.")
	AgendamentoResponse confirmar(
			@PathVariable UUID id,
			@Parameter(description = "Gestor ou profissional que confirma") @RequestHeader(USUARIO) UUID usuarioId) {
		return executarSemDados(confirmar, id, usuarioId);
	}

	@PostMapping("/concluir")
	@Operation(summary = "Conclui o atendimento", description = "Somente o gestor do estabelecimento ou o "
			+ "profissional. Só um agendamento CONFIRMADO pode ser concluído, e a partir do início do atendimento; "
			+ "os demais casos respondem 422.")
	AgendamentoResponse concluir(
			@PathVariable UUID id,
			@Parameter(description = "Gestor ou profissional que conclui") @RequestHeader(USUARIO) UUID usuarioId) {
		return executarSemDados(concluir, id, usuarioId);
	}

	@PostMapping("/cancelar")
	@Operation(summary = "Cancela o agendamento", description = "Somente o cliente ou o gestor do "
			+ "estabelecimento. O corpo é opcional; o motivo, de até 250 caracteres, fica no histórico. Só um "
			+ "agendamento AGENDADO ou CONFIRMADO pode ser cancelado; os demais status respondem 422. O horário "
			+ "volta a ficar livre.")
	AgendamentoResponse cancelar(
			@PathVariable UUID id,
			@Parameter(description = "Cliente ou gestor que cancela") @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody(required = false) CancelamentoRequest requisicao) {
		String motivo = requisicao == null ? null : requisicao.motivo();
		return AgendamentoResponse.de(cancelar.executar(new CancelarAgendamentoUseCase.Comando(id, usuarioId,
				motivo)));
	}

	/** Ações que recebem apenas o agendamento e o solicitante. */
	private static AgendamentoResponse executarSemDados(
			AlteracaoDeAgendamentoUseCase<ComandoDeAlteracao.Simples> casoDeUso, UUID agendamentoId,
			UUID usuarioSolicitante) {
		return AgendamentoResponse.de(casoDeUso.executar(
				new ComandoDeAlteracao.Simples(agendamentoId, usuarioSolicitante)));
	}

}
