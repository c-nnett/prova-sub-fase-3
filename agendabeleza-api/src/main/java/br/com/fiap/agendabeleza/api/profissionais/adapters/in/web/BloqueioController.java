package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.BloqueioRequest;
import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.BloqueioResponse;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.CriarBloqueioUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarBloqueiosUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.RemoverBloqueioUseCase;
import br.com.fiap.agendabeleza.api.profissionais.domain.BloqueioAgenda;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/profissionais/{profissionalId}/bloqueios")
@Tag(name = "Bloqueios", description = "Ausências pontuais do profissional, descontadas dos horários livres")
class BloqueioController {

	private static final String USUARIO = "X-Usuario-Id";
	private static final String DESCRICAO_USUARIO = "Identificador do usuário que faz a operação";
	private static final String GESTOR_OU_PROPRIO = "Gestor do estabelecimento ou o próprio profissional.";

	private final CriarBloqueioUseCase criar;
	private final ListarBloqueiosUseCase listar;
	private final RemoverBloqueioUseCase remover;

	BloqueioController(CriarBloqueioUseCase criar, ListarBloqueiosUseCase listar, RemoverBloqueioUseCase remover) {
		this.criar = criar;
		this.listar = listar;
		this.remover = remover;
	}

	@PostMapping
	@Operation(summary = "Cria um bloqueio", description = "O período é [inicio, fim). Responde 422 se o fim não "
			+ "for posterior ao início ou se o bloqueio já tiver terminado, e 409 se o profissional tiver agendamento "
			+ "ativo no período. " + GESTOR_OU_PROPRIO)
	ResponseEntity<BloqueioResponse> criar(
			@PathVariable UUID profissionalId,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody BloqueioRequest requisicao) {
		BloqueioAgenda criado = criar.executar(new CriarBloqueioUseCase.Comando(profissionalId, usuarioId,
				requisicao.inicio().toInstant(), requisicao.fim().toInstant(), requisicao.motivo()));
		URI local = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(local).body(BloqueioResponse.de(criado));
	}

	@GetMapping
	@Operation(summary = "Lista os bloqueios que ainda não terminaram",
			description = "Ordenados pelo início. " + GESTOR_OU_PROPRIO)
	List<BloqueioResponse> listar(
			@PathVariable UUID profissionalId,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId) {
		return listar.executar(profissionalId, usuarioId).stream().map(BloqueioResponse::de).toList();
	}

	@DeleteMapping("/{bloqueioId}")
	@Operation(summary = "Remove um bloqueio", description = "Responde 404 se o bloqueio não existir ou for de "
			+ "outro profissional. " + GESTOR_OU_PROPRIO)
	ResponseEntity<Void> remover(
			@PathVariable UUID profissionalId,
			@PathVariable UUID bloqueioId,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId) {
		remover.executar(new RemoverBloqueioUseCase.Comando(profissionalId, bloqueioId, usuarioId));
		return ResponseEntity.noContent().build();
	}

}
