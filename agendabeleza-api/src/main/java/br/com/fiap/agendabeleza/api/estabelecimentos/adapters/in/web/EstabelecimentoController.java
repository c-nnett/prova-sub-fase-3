package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.DadosEstabelecimentoRequest;
import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.EstabelecimentoRequest;
import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.EstabelecimentoResponse;
import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.FotoRequest;
import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.FotoResponse;
import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.HorarioDto;
import br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto.HorariosRequest;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AdicionarFotoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.AtualizarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.CadastrarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.ConsultarEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.DefinirHorariosFuncionamentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.RemoverFotoEstabelecimentoUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.FotoEstabelecimento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/estabelecimentos")
@Tag(name = "Estabelecimentos", description = "Cadastro, horários de funcionamento e fotos")
class EstabelecimentoController {

	static final String USUARIO = "X-Usuario-Id";
	private static final String DESCRICAO_USUARIO = "Identificador do usuário que faz a operação";

	private final CadastrarEstabelecimentoUseCase cadastrar;
	private final ConsultarEstabelecimentoUseCase consultar;
	private final AtualizarEstabelecimentoUseCase atualizar;
	private final DefinirHorariosFuncionamentoUseCase definirHorarios;
	private final AdicionarFotoEstabelecimentoUseCase adicionarFoto;
	private final RemoverFotoEstabelecimentoUseCase removerFoto;

	EstabelecimentoController(CadastrarEstabelecimentoUseCase cadastrar, ConsultarEstabelecimentoUseCase consultar,
			AtualizarEstabelecimentoUseCase atualizar, DefinirHorariosFuncionamentoUseCase definirHorarios,
			AdicionarFotoEstabelecimentoUseCase adicionarFoto, RemoverFotoEstabelecimentoUseCase removerFoto) {
		this.cadastrar = cadastrar;
		this.consultar = consultar;
		this.atualizar = atualizar;
		this.definirHorarios = definirHorarios;
		this.adicionarFoto = adicionarFoto;
		this.removerFoto = removerFoto;
	}

	@PostMapping
	@Operation(summary = "Cadastra um estabelecimento", description = "O usuário informado passa a ser o gestor.")
	ResponseEntity<EstabelecimentoResponse> cadastrar(
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody EstabelecimentoRequest requisicao) {
		Estabelecimento criado = cadastrar.executar(new CadastrarEstabelecimentoUseCase.Comando(
				usuarioId, requisicao.nome(), requisicao.descricao(), requisicao.endereco().paraDominio(),
				requisicao.timezone(), HorarioDto.paraDominio(requisicao.horarios()), requisicao.fotos()));
		URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.id()).toUri();
		return ResponseEntity.created(local).body(EstabelecimentoResponse.de(criado));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consulta os detalhes de um estabelecimento")
	EstabelecimentoResponse consultar(@PathVariable UUID id) {
		return EstabelecimentoResponse.de(consultar.executar(id));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Atualiza os dados cadastrais", description = "Somente o gestor do estabelecimento.")
	EstabelecimentoResponse atualizar(
			@PathVariable UUID id,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody DadosEstabelecimentoRequest requisicao) {
		return EstabelecimentoResponse.de(atualizar.executar(new AtualizarEstabelecimentoUseCase.Comando(
				id, usuarioId, requisicao.nome(), requisicao.descricao(), requisicao.endereco().paraDominio(),
				requisicao.timezone())));
	}

	@PutMapping("/{id}/horarios")
	@Operation(summary = "Define os horários de funcionamento",
			description = "Substitui todos os horários. Dia ausente significa fechado. Somente o gestor.")
	List<HorarioDto> definirHorarios(
			@PathVariable UUID id,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody HorariosRequest requisicao) {
		Estabelecimento estabelecimento = definirHorarios.executar(new DefinirHorariosFuncionamentoUseCase.Comando(
				id, usuarioId, HorarioDto.paraDominio(requisicao.horarios())));
		return estabelecimento.horarios().stream().map(HorarioDto::de).toList();
	}

	@PostMapping("/{id}/fotos")
	@Operation(summary = "Adiciona uma foto", description = "A foto é informada como URL pública. Somente o gestor.")
	ResponseEntity<FotoResponse> adicionarFoto(
			@PathVariable UUID id,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId,
			@Valid @RequestBody FotoRequest requisicao) {
		FotoEstabelecimento foto = adicionarFoto.executar(
				new AdicionarFotoEstabelecimentoUseCase.Comando(id, usuarioId, requisicao.url()));
		URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{fotoId}").buildAndExpand(foto.id()).toUri();
		return ResponseEntity.created(local).body(FotoResponse.de(foto));
	}

	@DeleteMapping("/{id}/fotos/{fotoId}")
	@Operation(summary = "Remove uma foto", description = "As fotos restantes são reordenadas. Somente o gestor.")
	ResponseEntity<Void> removerFoto(
			@PathVariable UUID id,
			@PathVariable UUID fotoId,
			@Parameter(description = DESCRICAO_USUARIO) @RequestHeader(USUARIO) UUID usuarioId) {
		removerFoto.executar(new RemoverFotoEstabelecimentoUseCase.Comando(id, usuarioId, fotoId));
		return ResponseEntity.noContent().build();
	}

}
