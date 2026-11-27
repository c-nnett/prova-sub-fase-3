package br.com.fiap.agendabeleza.api.calendario.adapters.in.web.dto;

import java.net.URI;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * As duas formas da mesma URL secreta do feed.
 *
 * @param url    para colar no Google Calendar ("Outras agendas" e depois "Do URL") ou no Outlook
 * @param webcal mesmo endereço com o esquema {@code webcal}, que abre o aplicativo de calendário ao ser clicado
 */
public record AssinaturaDoCalendarioResponse(
		@Schema(example = "https://api.exemplo.com/api/v1/calendario/feeds/Xy...Z.ics") String url,
		@Schema(example = "webcal://api.exemplo.com/api/v1/calendario/feeds/Xy...Z.ics") String webcal) {

	private static final String ESQUEMA_WEBCAL = "webcal";

	public static AssinaturaDoCalendarioResponse de(URI enderecoDoFeed) {
		String url = enderecoDoFeed.toString();
		String webcal = ESQUEMA_WEBCAL + url.substring(enderecoDoFeed.getScheme().length());
		return new AssinaturaDoCalendarioResponse(url, webcal);
	}

}
