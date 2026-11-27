package br.com.fiap.agendabeleza.api.calendario.domain;

import java.time.Instant;
import java.util.List;

/**
 * Conteúdo de uma agenda assinada, ainda sem formato. O {@code CalendarioGateway} o transforma em iCalendar.
 *
 * @param nome     nome que o calendário mostra para a agenda assinada
 * @param geradoEm instante da geração (o {@code DTSTAMP} dos eventos)
 * @param eventos  em ordem de início
 */
public record FeedDoCalendario(String nome, Instant geradoEm, List<EventoDoCalendario> eventos) {

	public FeedDoCalendario {
		eventos = List.copyOf(eventos);
	}

}
