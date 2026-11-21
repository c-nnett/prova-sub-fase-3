package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web;

import static br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.EstabelecimentoController.USUARIO;

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

import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.AtualizarServicoRequest;
import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.ServicoRequest;
import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.ServicoResponse;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AtualizarServicoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.CadastrarServicoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.ListarServicosUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Servico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/estabelecimentos/{estabelecimentoId}/servicos")
@Tag(name = "Serviços", description = "Serviços oferecidos pelos estabelecimentos")
class ServicoController {

	private static final String DESCRICAO_USUARIO = "Identificador do usuário que faz a operação";

	private final CadastrarServicoUseCase cadastrar;
	private final ListarServicosUseCase listar;
	private final AtualizarServicoUseCase atualizar;

	ServicoController(CadastrarServicoUseCase cadastrar, ListarServicosUseCase listar, AtualizarServicoUseCase atualizar) {
		this.cadastrar = cadastrar;
		this.listar = listar;
		this.atualizar = atualizar;
	}

	@PostMapping
	@Operation(summary = "Cadastra um serviço", description = "Somente o gestor do estabelecimento.")
	ResponseEntity<ServicoResponse> cadastrar(
			@PathVariable UUID estabelecimentoId,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody ServicoRequest requisicao) {
		Servico criado = cadastrar.executar(new CadastrarServicoUseCase.Comando(estabelecimentoId, usuarioId,
				requisicao.nome(), requisicao.descricao(), requisicao.categoria(), requisicao.duracaoMinutos(),
				requisicao.precoBase()));
		URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(local).body(ServicoResponse.de(criado));
	}

	@GetMapping
	@Operation(summary = "Lista os serviços ativos do estabelecimento", description = "Ordenados por nome.")
	List<ServicoResponse> listar(@PathVariable UUID estabelecimentoId) {
		return listar.executar(estabelecimentoId).stream().map(ServicoResponse::de).toList();
	}

	@PutMapping("/{servicoId}")
	@Operation(summary = "Atualiza um serviço", description = "Também ativa ou desativa. Somente o gestor.")
	ServicoResponse atualizar(
			@PathVariable UUID estabelecimentoId,
			@PathVariable UUID servicoId,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody AtualizarServicoRequest requisicao) {
		return ServicoResponse.de(atualizar.executar(new AtualizarServicoUseCase.Comando(estabelecimentoId, servicoId,
				usuarioId, requisicao.nome(), requisicao.descricao(), requisicao.categoria(),
				requisicao.duracaoMinutos(), requisicao.precoBase(), requisicao.ativo())));
	}

}
