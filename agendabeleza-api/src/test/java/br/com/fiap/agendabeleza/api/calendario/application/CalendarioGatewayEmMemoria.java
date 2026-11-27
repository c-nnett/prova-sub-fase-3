package br.com.fiap.agendabeleza.api.calendario.application;

import br.com.fiap.agendabeleza.api.calendario.application.port.CalendarioGateway;
import br.com.fiap.agendabeleza.api.calendario.domain.FeedDoCalendario;

/**
 * Guarda o último feed recebido para o teste inspecionar o conteúdo sem depender do formato iCalendar.
 */
public class CalendarioGatewayEmMemoria implements CalendarioGateway {

	public static final String CONTEUDO_GERADO = "conteudo-do-feed";

	private FeedDoCalendario ultimoFeed;

	@Override
	public String gerar(FeedDoCalendario feed) {
		this.ultimoFeed = feed;
		return CONTEUDO_GERADO;
	}

	public FeedDoCalendario ultimoFeed() {
		return ultimoFeed;
	}

}
