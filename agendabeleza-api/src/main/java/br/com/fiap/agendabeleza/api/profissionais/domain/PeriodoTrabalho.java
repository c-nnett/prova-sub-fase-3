package br.com.fiap.agendabeleza.api.profissionais.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;

import java.time.DayOfWeek;
import java.time.LocalTime;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Período da jornada semanal recorrente do profissional, no fuso do estabelecimento. Um dia pode ter mais de um
 * período, como manhã e tarde.
 */
public record PeriodoTrabalho(DayOfWeek diaSemana, LocalTime inicio, LocalTime fim) {

	public PeriodoTrabalho {
		obrigatorio(diaSemana, "diaSemana");
		obrigatorio(inicio, "inicio");
		obrigatorio(fim, "fim");
		if (!fim.isAfter(inicio)) {
			throw new RegraNegocioException("fim deve ser posterior a inicio");
		}
	}

	/** Indica se os dois períodos caem no mesmo dia e se cruzam. Períodos que só se encostam não se sobrepõem. */
	public boolean sobrepoe(PeriodoTrabalho outro) {
		return diaSemana == outro.diaSemana && inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
	}

}
