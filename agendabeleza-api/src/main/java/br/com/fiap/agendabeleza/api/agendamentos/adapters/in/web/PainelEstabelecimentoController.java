package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto.AgendamentoResponse;
import br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto.ResumoDaAgendaResponse;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarAgendaDoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ResumirAgendaDoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Painel do estabelecimento (funcionalidade 6). Não há frontend no escopo: o painel são estes endpoints, documentados
 * no Swagger (arquitetura, seção 1).
 */
@RestController
@RequestMapping("/api/v1/estabelecimentos/{estabelecimentoId}")
@Tag(name = "Painel do estabelecimento", description = "Agenda filtrada e resumo do período, para o gestor")
class PainelEstabelecimentoController {

	private static final String USUARIO = "X-Usuario-Id";
	private static final String REGRAS_DO_PERIODO = "As datas são dias no fuso do estabelecimento, e o período "
			+ "inclui o primeiro e o último dia. Sem datas, vale o dia de hoje; só com uma delas, vale esse dia. "
			+ "Responde 400 para data, status ou identificador malformado, 403 se o usuário não for o gestor do "
			+ "estabelecimento, 404 se o estabelecimento não existir e 422 se a data final for anterior à inicial "
			+ "ou se o período passar de 31 dias.";

	private final ConsultarAgendaDoEstabelecimentoUseCase consultarAgenda;
	private final ResumirAgendaDoEstabelecimentoUseCase resumirAgenda;

	PainelEstabelecimentoController(ConsultarAgendaDoEstabelecimentoUseCase consultarAgenda,
			ResumirAgendaDoEstabelecimentoUseCase resumirAgenda) {
		this.consultarAgenda = consultarAgenda;
		this.resumirAgenda = resumirAgenda;
	}

	@GetMapping("/agendamentos")
	@Operation(summary = "Agenda do estabelecimento", description = "Agendamentos que começam no período, em ordem "
			+ "de início, opcionalmente filtrados por status (repita o parâmetro para vários) e por profissional. "
			+ REGRAS_DO_PERIODO)
	List<AgendamentoResponse> consultarAgenda(
			@PathVariable UUID estabelecimentoId,
			@Parameter(description = "Identificador do gestor") @RequestHeader(USUARIO) UUID usuarioId,
			@Parameter(description = "Primeiro dia (aaaa-mm-dd)") @RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@Parameter(description = "Último dia, inclusivo (aaaa-mm-dd)") @RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
			@Parameter(description = "Status desejados; sem o parâmetro, todos") @RequestParam(required = false)
			Set<StatusAgendamento> status,
			@Parameter(description = "Somente deste profissional") @RequestParam(required = false)
			UUID profissionalId) {
		return consultarAgenda.executar(new ConsultarAgendaDoEstabelecimentoUseCase.Consulta(estabelecimentoId,
				usuarioId, de, ate, Objects.requireNonNullElse(status, Set.of()), profissionalId))
				.dados().stream()
				.map(AgendamentoResponse::de)
				.toList();
	}

	@GetMapping("/painel/resumo")
	@Operation(summary = "Resumo do período", description = "Total de agendamentos que começam no período, total "
			+ "por status (todos aparecem, com zero quando não houver), receita realizada (CONCLUIDO) e receita "
			+ "prevista (AGENDADO e CONFIRMADO), opcionalmente de um profissional. " + REGRAS_DO_PERIODO)
	ResumoDaAgendaResponse resumir(
			@PathVariable UUID estabelecimentoId,
			@Parameter(description = "Identificador do gestor") @RequestHeader(USUARIO) UUID usuarioId,
			@Parameter(description = "Primeiro dia (aaaa-mm-dd)") @RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@Parameter(description = "Último dia, inclusivo (aaaa-mm-dd)") @RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
			@Parameter(description = "Somente deste profissional") @RequestParam(required = false)
			UUID profissionalId) {
		return ResumoDaAgendaResponse.de(resumirAgenda.executar(new ResumirAgendaDoEstabelecimentoUseCase.Consulta(
				estabelecimentoId, usuarioId, de, ate, profissionalId)));
	}

}
