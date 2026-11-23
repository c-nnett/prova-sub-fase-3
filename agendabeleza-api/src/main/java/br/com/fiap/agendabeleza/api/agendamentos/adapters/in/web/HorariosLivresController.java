package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto.HorariosLivresResponse;
import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarHorariosLivresUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Agendamentos", description = "Horários livres, reservas e gestão da agenda")
class HorariosLivresController {

	private final ConsultarHorariosLivresUseCase consultar;

	HorariosLivresController(ConsultarHorariosLivresUseCase consultar) {
		this.consultar = consultar;
	}

	@GetMapping("/profissionais/{id}/horarios-livres")
	@Operation(summary = "Lista os horários livres do profissional para um serviço em uma data",
			description = "Consulta pública. Cruza a jornada do profissional, o funcionamento do estabelecimento e os "
					+ "bloqueios, em passos de 15 minutos, no fuso do estabelecimento. Horários que já começaram "
					+ "não aparecem. Responde 404 se o profissional ou o serviço não existir e 422 se algum deles "
					+ "estiver inativo ou se o profissional não executar o serviço.")
	HorariosLivresResponse consultar(
			@PathVariable UUID id,
			@Parameter(description = "Serviço que será agendado") @RequestParam UUID servicoId,
			@Parameter(description = "Data no fuso do estabelecimento", example = "2026-11-30")
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
		return HorariosLivresResponse.de(consultar.executar(
				new ConsultarHorariosLivresUseCase.Consulta(id, servicoId, data)));
	}

}
