package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto.EspecialidadeResponse;
import br.com.fiap.agendabeleza.api.profissionais.application.usecase.ListarEspecialidadesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/especialidades")
@Tag(name = "Especialidades", description = "Catálogo de especialidades dos profissionais")
class EspecialidadeController {

	private final ListarEspecialidadesUseCase listar;

	EspecialidadeController(ListarEspecialidadesUseCase listar) {
		this.listar = listar;
	}

	@GetMapping
	@Operation(summary = "Lista o catálogo de especialidades", description = "Consulta pública, ordenada por nome.")
	List<EspecialidadeResponse> listar() {
		return listar.executar().stream().map(EspecialidadeResponse::de).toList();
	}

}
