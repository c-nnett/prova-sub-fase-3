package br.com.fiap.agendabeleza.api.estabelecimentos.adapters.in.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import br.com.fiap.agendabeleza.api.estabelecimentos.domain.HorarioFuncionamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record HorarioDto(
		@NotNull @Schema(example = "MONDAY") DayOfWeek diaSemana,
		@NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "09:00") LocalTime abreAs,
		@NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:00") LocalTime fechaAs) {

	public HorarioFuncionamento paraDominio() {
		return new HorarioFuncionamento(diaSemana, abreAs, fechaAs);
	}

	public static HorarioDto de(HorarioFuncionamento horario) {
		return new HorarioDto(horario.diaSemana(), horario.abreAs(), horario.fechaAs());
	}

	public static List<HorarioFuncionamento> paraDominio(List<HorarioDto> horarios) {
		return horarios == null ? List.of() : horarios.stream().map(HorarioDto::paraDominio).toList();
	}

}
