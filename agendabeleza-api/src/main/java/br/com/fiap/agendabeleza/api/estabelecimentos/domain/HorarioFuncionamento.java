package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;

import java.time.DayOfWeek;
import java.time.LocalTime;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Horário de funcionamento de um dia da semana, no fuso do estabelecimento. Dia sem horário significa fechado.
 */
public record HorarioFuncionamento(DayOfWeek diaSemana, LocalTime abreAs, LocalTime fechaAs) {

	public HorarioFuncionamento {
		obrigatorio(diaSemana, "diaSemana");
		obrigatorio(abreAs, "abreAs");
		obrigatorio(fechaAs, "fechaAs");
		if (!fechaAs.isAfter(abreAs)) {
			throw new RegraNegocioException("fechaAs deve ser posterior a abreAs");
		}
	}

	/** Indica se o intervalo {@code [inicio, fim]} cabe inteiro no funcionamento. */
	public boolean abrange(LocalTime inicio, LocalTime fim) {
		return !inicio.isBefore(abreAs) && !fim.isAfter(fechaAs);
	}

}
