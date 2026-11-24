package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;

/**
 * Horários livres de um profissional em uma data, para um serviço. O fuso é o do estabelecimento, usado para
 * apresentar os horários.
 */
public record HorariosLivres(LocalDate data, ZoneId fuso, int duracaoMinutos, List<Intervalo> horarios) {

	public HorariosLivres {
		horarios = List.copyOf(horarios);
	}

}
