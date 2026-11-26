package br.com.fiap.agendabeleza.api.calendario.adapters.in.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.fiap.agendabeleza.api.calendario.adapters.in.web.dto.AssinaturaDoCalendarioResponse;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.EmitirAssinaturaDoCalendarioUseCase;
import br.com.fiap.agendabeleza.api.calendario.application.usecase.GerarFeedDoCalendarioUseCase;
import br.com.fiap.agendabeleza.api.calendario.domain.PapelNoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.TokenDoCalendario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/calendario")
@Tag(name = "Calendário", description = "Feed iCalendar (.ics) da agenda, assinado pelo Google Calendar ou Outlook "
		+ "por uma URL secreta")
class CalendarioController {

	private static final String USUARIO = "X-Usuario-Id";
	private static final String CAMINHO_DO_FEED = "/api/v1/calendario/feeds/{token}.ics";
	private static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);
	private static final String NOME_DO_ARQUIVO = "agenda.ics";

	private final EmitirAssinaturaDoCalendarioUseCase emitir;
	private final GerarFeedDoCalendarioUseCase gerarFeed;

	CalendarioController(EmitirAssinaturaDoCalendarioUseCase emitir, GerarFeedDoCalendarioUseCase gerarFeed) {
		this.emitir = emitir;
		this.gerarFeed = gerarFeed;
	}

	@PostMapping("/assinaturas/profissionais/{profissionalId}")
	@Operation(summary = "Gera a URL secreta da agenda do profissional",
			description = "Somente o gestor do estabelecimento ou o próprio profissional. Cada chamada gera uma URL "
					+ "nova e a anterior deixa de funcionar. Responde 400 sem o cabeçalho do usuário, 403 para os "
					+ "demais usuários e 404 se o profissional não existir.")
	ResponseEntity<AssinaturaDoCalendarioResponse> assinarAgendaDoProfissional(@PathVariable UUID profissionalId,
			@Parameter(description = "Gestor ou o próprio profissional") @RequestHeader(USUARIO) UUID usuarioId) {
		return emitida(new EmitirAssinaturaDoCalendarioUseCase.Comando(PapelNoCalendario.PROFISSIONAL,
				profissionalId, usuarioId));
	}

	@PostMapping("/assinaturas/cliente")
	@Operation(summary = "Gera a URL secreta dos horários do cliente",
			description = "O usuário do cabeçalho é o cliente: o feed traz os agendamentos dele em todos os "
					+ "estabelecimentos. Cada chamada gera uma URL nova e a anterior deixa de funcionar. Responde 400 "
					+ "sem o cabeçalho do usuário.")
	ResponseEntity<AssinaturaDoCalendarioResponse> assinarHorariosDoCliente(
			@Parameter(description = "Identificador do cliente") @RequestHeader(USUARIO) UUID usuarioId) {
		return emitida(new EmitirAssinaturaDoCalendarioUseCase.Comando(PapelNoCalendario.CLIENTE, usuarioId,
				usuarioId));
	}

	@GetMapping("/feeds/{token}.ics")
	@Operation(summary = "Feed iCalendar da agenda",
			description = "Público: o token da URL é a credencial, porque o Google Calendar e o Outlook não mandam "
					+ "cabeçalhos ao buscar a agenda. Traz os agendamentos que terminam a partir de 30 dias atrás, sem "
					+ "os cancelados, com horários em UTC. Responde 404 se o token for inválido, desconhecido ou já "
					+ "tiver sido trocado.")
	ResponseEntity<String> feed(@PathVariable String token) {
		return ResponseEntity.ok()
				.contentType(TEXT_CALENDAR)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.inline().filename(NOME_DO_ARQUIVO).build().toString())
				.body(gerarFeed.executar(token));
	}

	/**
	 * Monta a URL a partir do endereço que o cliente usou. Atrás de proxy (túnel, plataforma em nuvem), os
	 * cabeçalhos {@code X-Forwarded-*} entram por {@code server.forward-headers-strategy=framework}.
	 */
	private ResponseEntity<AssinaturaDoCalendarioResponse> emitida(EmitirAssinaturaDoCalendarioUseCase.Comando comando) {
		TokenDoCalendario token = emitir.executar(comando);
		URI enderecoDoFeed = ServletUriComponentsBuilder.fromCurrentContextPath().path(CAMINHO_DO_FEED)
				.buildAndExpand(token.valor()).toUri();
		return ResponseEntity.created(enderecoDoFeed).body(AssinaturaDoCalendarioResponse.de(enderecoDoFeed));
	}

}
