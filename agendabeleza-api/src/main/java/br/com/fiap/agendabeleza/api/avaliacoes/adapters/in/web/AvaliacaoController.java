package br.com.fiap.agendabeleza.api.avaliacoes.adapters.in.web;

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

import br.com.fiap.agendabeleza.api.avaliacoes.adapters.in.web.dto.AvaliacaoRequest;
import br.com.fiap.agendabeleza.api.avaliacoes.adapters.in.web.dto.AvaliacaoResponse;
import br.com.fiap.agendabeleza.api.avaliacoes.application.usecase.AvaliarAtendimentoUseCase;
import br.com.fiap.agendabeleza.api.avaliacoes.application.usecase.ListarAvaliacoesUseCase;
import br.com.fiap.agendabeleza.api.avaliacoes.domain.Avaliacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Avaliações e comentários (funcionalidade 4). A avaliação fica sob o agendamento, e as listas sob o estabelecimento e
 * o profissional avaliados (arquitetura, seção 5.4).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Avaliações", description = "Avaliação de atendimentos concluídos e listas públicas de avaliações")
class AvaliacaoController {

	private static final String USUARIO = "X-Usuario-Id";

	private final AvaliarAtendimentoUseCase avaliar;
	private final ListarAvaliacoesUseCase listar;

	AvaliacaoController(AvaliarAtendimentoUseCase avaliar, ListarAvaliacoesUseCase listar) {
		this.avaliar = avaliar;
		this.listar = listar;
	}

	@PostMapping("/agendamentos/{agendamentoId}/avaliacao")
	@Operation(summary = "Avalia um atendimento concluído", description = "Somente o cliente do agendamento, uma vez "
			+ "por atendimento. Recalcula as notas médias do estabelecimento e do profissional na mesma transação. "
			+ "Responde 400 para nota ausente ou fora de 1 a 5 e comentário com mais de 1000 caracteres, 404 se o "
			+ "agendamento não existir, 403 se o usuário não for o cliente e 422 se o atendimento não estiver "
			+ "concluído ou já tiver sido avaliado.")
	ResponseEntity<AvaliacaoResponse> avaliar(
			@PathVariable UUID agendamentoId,
			@Parameter(description = "Identificador do cliente do agendamento") @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody AvaliacaoRequest requisicao) {
		Avaliacao avaliacao = avaliar.executar(new AvaliarAtendimentoUseCase.Comando(agendamentoId, usuarioId,
				requisicao.notaEstabelecimento(), requisicao.notaProfissional(), requisicao.comentario()));
		URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/estabelecimentos/{estabelecimentoId}/avaliacoes")
				.buildAndExpand(avaliacao.estabelecimentoId()).toUri();
		return ResponseEntity.created(local).body(AvaliacaoResponse.de(avaliacao));
	}

	@GetMapping("/estabelecimentos/{estabelecimentoId}/avaliacoes")
	@Operation(summary = "Lista as avaliações do estabelecimento",
			description = "Pública, da mais recente para a mais antiga. Responde 404 se o estabelecimento não existir.")
	List<AvaliacaoResponse> listarDoEstabelecimento(@PathVariable UUID estabelecimentoId) {
		return paraResposta(listar.doEstabelecimento(estabelecimentoId));
	}

	@GetMapping("/profissionais/{profissionalId}/avaliacoes")
	@Operation(summary = "Lista as avaliações do profissional",
			description = "Pública, da mais recente para a mais antiga. Responde 404 se o profissional não existir.")
	List<AvaliacaoResponse> listarDoProfissional(@PathVariable UUID profissionalId) {
		return paraResposta(listar.doProfissional(profissionalId));
	}

	private static List<AvaliacaoResponse> paraResposta(List<Avaliacao> avaliacoes) {
		return avaliacoes.stream().map(AvaliacaoResponse::de).toList();
	}

}
