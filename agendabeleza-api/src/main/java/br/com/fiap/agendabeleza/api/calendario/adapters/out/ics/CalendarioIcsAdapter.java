package br.com.fiap.agendabeleza.api.calendario.adapters.out.ics;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import br.com.fiap.agendabeleza.api.calendario.application.port.CalendarioGateway;
import br.com.fiap.agendabeleza.api.calendario.domain.EventoDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.FeedDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.SituacaoDoEvento;

/**
 * Implementa {@link CalendarioGateway} escrevendo iCalendar (RFC 5545) à mão, sem biblioteca: o formato usado é
 * pequeno, e uma dependência a mais seria risco no Spring Boot 4.1 a dois dias da entrega (decisão de 26/11).
 * <ul>
 * <li>Datas em UTC ({@code Z}): dispensa o bloco {@code VTIMEZONE}, e cada calendário mostra no fuso de quem
 * assina.</li>
 * <li>{@code UID} fixo por agendamento e {@code SEQUENCE} = versão: uma alteração troca o evento em vez de
 * duplicá-lo.</li>
 * <li>Texto escapado (seção 3.3.11) e linhas dobradas em 75 octetos (seção 3.1), sempre terminadas em CRLF.</li>
 * </ul>
 * {@code REFRESH-INTERVAL} e {@code X-PUBLISHED-TTL} pedem atualização a cada hora. O Outlook respeita; o Google
 * Calendar decide sozinho (em geral várias horas).
 */
@Component
class CalendarioIcsAdapter implements CalendarioGateway {

	private static final String QUEBRA = "\r\n";
	private static final int LIMITE_DE_OCTETOS_POR_LINHA = 75;
	private static final String DOMINIO_DO_UID = "@agendabeleza";
	private static final DateTimeFormatter DATA_HORA_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
			.withZone(ZoneOffset.UTC);

	@Override
	public String gerar(FeedDoCalendario feed) {
		StringBuilder documento = new StringBuilder();
		linha(documento, "BEGIN:VCALENDAR");
		linha(documento, "VERSION:2.0");
		linha(documento, "PRODID:-//AgendaBeleza//API//PT-BR");
		linha(documento, "CALSCALE:GREGORIAN");
		linha(documento, "METHOD:PUBLISH");
		linha(documento, "X-WR-CALNAME:" + escapar(feed.nome()));
		linha(documento, "REFRESH-INTERVAL;VALUE=DURATION:PT1H");
		linha(documento, "X-PUBLISHED-TTL:PT1H");
		feed.eventos().forEach(evento -> evento(documento, evento, feed.geradoEm()));
		linha(documento, "END:VCALENDAR");
		return documento.toString();
	}

	private static void evento(StringBuilder documento, EventoDoCalendario evento, Instant geradoEm) {
		linha(documento, "BEGIN:VEVENT");
		linha(documento, "UID:" + evento.agendamentoId() + DOMINIO_DO_UID);
		linha(documento, "DTSTAMP:" + emUtc(geradoEm));
		linha(documento, "DTSTART:" + emUtc(evento.inicio()));
		linha(documento, "DTEND:" + emUtc(evento.fim()));
		linha(documento, "SUMMARY:" + escapar(evento.detalhes().resumo()));
		linha(documento, "LOCATION:" + escapar(evento.detalhes().local()));
		linha(documento, "DESCRIPTION:" + escapar(evento.detalhes().descricao()));
		linha(documento, "STATUS:" + status(evento.situacao()));
		linha(documento, "SEQUENCE:" + evento.sequencia());
		linha(documento, "END:VEVENT");
	}

	private static String status(SituacaoDoEvento situacao) {
		return switch (situacao) {
			case PROVISORIO -> "TENTATIVE";
			case CONFIRMADO -> "CONFIRMED";
		};
	}

	private static String emUtc(Instant instante) {
		return DATA_HORA_UTC.format(instante);
	}

	/** Barra invertida primeiro, para não escapar de novo as barras que os outros escapes inserem. */
	private static String escapar(String texto) {
		return texto.replace("\\", "\\\\")
				.replace(";", "\\;")
				.replace(",", "\\,")
				.replace("\r\n", "\\n")
				.replace("\n", "\\n");
	}

	/**
	 * Acrescenta a linha dobrada: ao passar de 75 octetos em UTF-8, quebra e continua com um espaço, que conta no
	 * limite da linha seguinte. Anda por code point, então um caractere acentuado nunca fica dividido.
	 */
	private static void linha(StringBuilder documento, String conteudo) {
		int octetosNaLinha = 0;
		for (int indice = 0; indice < conteudo.length();) {
			int caractere = conteudo.codePointAt(indice);
			int octetos = Character.toString(caractere).getBytes(StandardCharsets.UTF_8).length;
			if (octetosNaLinha + octetos > LIMITE_DE_OCTETOS_POR_LINHA) {
				documento.append(QUEBRA).append(' ');
				octetosNaLinha = 1;
			}
			documento.appendCodePoint(caractere);
			octetosNaLinha += octetos;
			indice += Character.charCount(caractere);
		}
		documento.append(QUEBRA);
	}

}
