package br.com.fiap.agendabeleza.api.profissionais.adapters.in.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.fiap.agendabeleza.api.profissionais.domain.PeriodoTrabalho;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PeriodoDto(
		@NotNull @Schema(example = "MONDAY") DayOfWeek diaSemana,
		@NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "09:00") LocalTime inicio,
		@NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "12:00") LocalTime fim) {

	public PeriodoTrabalho paraDominio() {
		return new PeriodoTrabalho(diaSemana, inicio, fim);
	}

	public static PeriodoDto de(PeriodoTrabalho periodo) {
		return new PeriodoDto(periodo.diaSemana(), periodo.inicio(), periodo.fim());
	}

}
