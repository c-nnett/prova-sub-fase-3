package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto.AgendamentoRequest;
import br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto.AgendamentoResponse;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.CriarAgendamentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ListarMeusAgendamentosUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/agendamentos")
@Tag(name = "Agendamentos", description = "Reserva de horários e consulta dos agendamentos")
class AgendamentoController {

	private static final String USUARIO = "X-Usuario-Id";

	private final CriarAgendamentoUseCase criar;
	private final ListarMeusAgendamentosUseCase listarMeus;
	private final ConsultarAgendamentoUseCase consultar;

	AgendamentoController(CriarAgendamentoUseCase criar, ListarMeusAgendamentosUseCase listarMeus,
			ConsultarAgendamentoUseCase consultar) {
		this.criar = criar;
		this.listarMeus = listarMeus;
		this.consultar = consultar;
	}

	@PostMapping
	@Operation(summary = "Reserva um horário", description = "O usuário do cabeçalho é o cliente. O preço é a "
			+ "tarifa do profissional para o serviço. Responde 404 se o profissional ou o serviço não existir, 422 "
			+ "se o horário estiver no passado, fora do atendimento ou em um bloqueio do profissional, ou se ele não "
			+ "executar o serviço, "
			+ "e 409 se o profissional já tiver um agendamento que se sobrepõe ao horário.")
	ResponseEntity<AgendamentoResponse> criar(
			@Parameter(description = "Identificador do cliente que faz a reserva") @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody AgendamentoRequest requisicao) {
		Agendamento criado = criar.executar(new CriarAgendamentoUseCase.Comando(usuarioId,
				requisicao.profissionalId(), requisicao.servicoId(), requisicao.inicio(), requisicao.observacoes()));
		URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/agendamentos/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(local).body(AgendamentoResponse.de(criado));
	}

	@GetMapping("/meus")
	@Operation(summary = "Lista os agendamentos do cliente",
			description = "Todos os status, do início mais recente para o mais antigo.")
	List<AgendamentoResponse> listarMeus(
			@Parameter(description = "Identificador do cliente") @RequestHeader(USUARIO) UUID usuarioId) {
		return listarMeus.executar(usuarioId).stream().map(AgendamentoResponse::de).toList();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta um agendamento", description = "Somente o cliente, o gestor do estabelecimento "
			+ "ou o profissional. Responde 403 para os demais usuários e 404 se o agendamento não existir.")
	AgendamentoResponse consultar(
			@PathVariable UUID id,
			@Parameter(description = "Identificador do usuário que consulta") @RequestHeader(USUARIO) UUID usuarioId) {
		return AgendamentoResponse.de(consultar.executar(id, usuarioId));
	}

}
