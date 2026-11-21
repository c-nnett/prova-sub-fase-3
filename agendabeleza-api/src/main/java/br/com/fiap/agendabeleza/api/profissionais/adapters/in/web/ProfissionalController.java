package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.AtualizarProfissionalRequest;
import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.EspecialidadesRequest;
import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.JornadaRequest;
import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.ProfissionalRequest;
import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.ProfissionalResponse;
import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.ServicosProfissionalRequest;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.AtualizarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.CadastrarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ConsultarProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirEspecialidadesUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirJornadaUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DefinirServicosProfissionalUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.DetalharProfissionaisUseCase;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarProfissionaisUseCase;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Profissionais", description = "Perfil, especialidades, serviços com tarifa e jornada semanal")
class ProfissionalController {

	private static final String USUARIO = "X-Usuario-Id";
	private static final String DESCRICAO_USUARIO = "Identificador do usuário que faz a operação";
	private static final String GESTOR_OU_PROPRIO = "Gestor do estabelecimento ou o próprio profissional.";

	private final CadastrarProfissionalUseCase cadastrar;
	private final ListarProfissionaisUseCase listar;
	private final ConsultarProfissionalUseCase consultar;
	private final AtualizarProfissionalUseCase atualizar;
	private final DefinirEspecialidadesUseCase definirEspecialidades;
	private final DefinirServicosProfissionalUseCase definirServicos;
	private final DefinirJornadaUseCase definirJornada;
	private final DetalharProfissionaisUseCase detalhar;

	@SuppressWarnings("java:S107") // Um caso de uso por operação, injetados pelo construtor.
	ProfissionalController(CadastrarProfissionalUseCase cadastrar, ListarProfissionaisUseCase listar,
			ConsultarProfissionalUseCase consultar, AtualizarProfissionalUseCase atualizar,
			DefinirEspecialidadesUseCase definirEspecialidades, DefinirServicosProfissionalUseCase definirServicos,
			DefinirJornadaUseCase definirJornada, DetalharProfissionaisUseCase detalhar) {
		this.cadastrar = cadastrar;
		this.listar = listar;
		this.consultar = consultar;
		this.atualizar = atualizar;
		this.definirEspecialidades = definirEspecialidades;
		this.definirServicos = definirServicos;
		this.definirJornada = definirJornada;
		this.detalhar = detalhar;
	}

	@PostMapping("/estabelecimentos/{estabelecimentoId}/profissionais")
	@Operation(summary = "Cadastra um profissional", description = "Somente o gestor do estabelecimento. "
			+ "Responde 409 se o usuarioId já estiver vinculado a outro profissional.")
	ResponseEntity<ProfissionalResponse> cadastrar(
			@PathVariable UUID estabelecimentoId,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody ProfissionalRequest requisicao) {
		Profissional criado = cadastrar.executar(new CadastrarProfissionalUseCase.Comando(estabelecimentoId,
				usuarioId, requisicao.usuarioId(), requisicao.nome(), requisicao.email(), requisicao.bio()));
		URI local = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/profissionais/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(local).body(completo(criado));
	}

	@GetMapping("/estabelecimentos/{estabelecimentoId}/profissionais")
	@Operation(summary = "Lista os profissionais ativos do estabelecimento",
			description = "Consulta pública, ordenada por nome e sem o e-mail.")
	List<ProfissionalResponse> listar(@PathVariable UUID estabelecimentoId) {
		return detalhar.executar(listar.executar(estabelecimentoId)).stream()
				.map(ProfissionalResponse::publico)
				.toList();
	}

	@GetMapping("/profissionais/{id}")
	@Operation(summary = "Consulta o perfil de um profissional", description = "Consulta pública, sem o e-mail.")
	ProfissionalResponse consultar(@PathVariable UUID id) {
		return ProfissionalResponse.publico(detalhar.executar(consultar.executar(id)));
	}

	@PutMapping("/profissionais/{id}")
	@Operation(summary = "Atualiza o perfil", description = "Também ativa ou desativa. " + GESTOR_OU_PROPRIO)
	ProfissionalResponse atualizar(
			@PathVariable UUID id,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody AtualizarProfissionalRequest requisicao) {
		return completo(atualizar.executar(new AtualizarProfissionalUseCase.Comando(id, usuarioId,
				requisicao.nome(), requisicao.email(), requisicao.bio(), requisicao.ativo())));
	}

	@PutMapping("/profissionais/{id}/especialidades")
	@Operation(summary = "Define as especialidades",
			description = "Substitui todas. Os ids vêm de GET /especialidades. " + GESTOR_OU_PROPRIO)
	ProfissionalResponse definirEspecialidades(
			@PathVariable UUID id,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody EspecialidadesRequest requisicao) {
		return completo(definirEspecialidades.executar(
				new DefinirEspecialidadesUseCase.Comando(id, usuarioId, requisicao.especialidadeIds())));
	}

	@PutMapping("/profissionais/{id}/servicos")
	@Operation(summary = "Define os serviços e as tarifas",
			description = "Substitui todos. Cada serviço precisa ser do estabelecimento e estar ativo. "
					+ "Tarifa ausente assume o preço base do serviço. " + GESTOR_OU_PROPRIO)
	ProfissionalResponse definirServicos(
			@PathVariable UUID id,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody ServicosProfissionalRequest requisicao) {
		return completo(definirServicos.executar(
				new DefinirServicosProfissionalUseCase.Comando(id, usuarioId, requisicao.paraItens())));
	}

	@PutMapping("/profissionais/{id}/disponibilidades")
	@Operation(summary = "Define a jornada semanal",
			description = "Substitui todos os períodos. Cada um precisa caber no funcionamento do estabelecimento, "
					+ "e os períodos de um dia não podem se sobrepor. " + GESTOR_OU_PROPRIO)
	ProfissionalResponse definirJornada(
			@PathVariable UUID id,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody JornadaRequest requisicao) {
		return completo(definirJornada.executar(
				new DefinirJornadaUseCase.Comando(id, usuarioId, requisicao.paraDominio())));
	}

	private ProfissionalResponse completo(Profissional profissional) {
		return ProfissionalResponse.completo(detalhar.executar(profissional));
	}

}
