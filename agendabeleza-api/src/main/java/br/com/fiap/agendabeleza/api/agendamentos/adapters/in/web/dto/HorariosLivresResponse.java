package br.com.fiap.agendabeleza.api.agendamentos.adapters.in.web.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.HorariosLivres;
import io.swagger.v3.oas.annotations.media.Schema;

public record HorariosLivresResponse(
		LocalDate data,
		@Schema(description = "Fuso do estabelecimento", example = "America/Sao_Paulo") String fuso,
		@Schema(description = "Duração do serviço consultado") int duracaoMinutos,
		List<Horario> horarios) {

	public static HorariosLivresResponse de(HorariosLivres livres) {
		ZoneId fuso = livres.fuso();
		List<Horario> horarios = livres.horarios().stream()
				.map(horario -> new Horario(horario.inicio().atZone(fuso).toOffsetDateTime(),
						horario.fim().atZone(fuso).toOffsetDateTime()))
				.toList();
		return new HorariosLivresResponse(livres.data(), fuso.getId(), livres.duracaoMinutos(), horarios);
	}

	public record Horario(
			@Schema(example = "2026-11-30T09:00:00-03:00") OffsetDateTime inicio,
			@Schema(description = "Fim, exclusivo", example = "2026-11-30T10:00:00-03:00") OffsetDateTime fim) {
	}

}
