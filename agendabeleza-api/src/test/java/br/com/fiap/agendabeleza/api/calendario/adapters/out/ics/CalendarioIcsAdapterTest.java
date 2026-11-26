package br.com.fiap.agendabeleza.api.calendario.adapters.out.ics;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.calendario.domain.DetalhesDoEvento;
import br.com.fiap.agendabeleza.api.calendario.domain.EventoDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.FeedDoCalendario;
import br.com.fiap.agendabeleza.api.calendario.domain.SituacaoDoEvento;

class CalendarioIcsAdapterTest {

	private static final String QUEBRA = "\r\n";
	private static final int LIMITE_DE_OCTETOS = 75;
	private static final UUID AGENDAMENTO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
	private static final Instant GERADO_EM = Instant.parse("2030-03-04T12:00:00Z");
	private static final Instant INICIO = Instant.parse("2030-03-05T13:00:00Z");
	private static final Instant FIM = Instant.parse("2030-03-05T13:45:00Z");

	private final CalendarioIcsAdapter adaptador = new CalendarioIcsAdapter();

	private static EventoDoCalendario evento(DetalhesDoEvento detalhes, SituacaoDoEvento situacao) {
		return new EventoDoCalendario(AGENDAMENTO_ID, INICIO, FIM, detalhes, situacao, 3);
	}

	private String gerar(EventoDoCalendario... eventos) {
		return adaptador.gerar(new FeedDoCalendario("AgendaBeleza - Ana Souza", GERADO_EM, List.of(eventos)));
	}

	/** Desfaz a dobra de linha do RFC 5545 (seção 3.1): quebra seguida de espaço não faz parte do conteúdo. */
	private static String desdobrar(String documento) {
		return documento.replace(QUEBRA + " ", "");
	}

	@Test
	void geraODocumentoCompletoComDatasEmUtcEQuebrasCrlf() {
		String documento = gerar(evento(new DetalhesDoEvento("Corte feminino", "Rua Augusta, 100 - Centro",
				"Serviço: Corte\nPreço: R$ 80,00"), SituacaoDoEvento.CONFIRMADO));

		assertThat(documento).isEqualTo(String.join(QUEBRA,
				"BEGIN:VCALENDAR",
				"VERSION:2.0",
				"PRODID:-//AgendaBeleza//API//PT-BR",
				"CALSCALE:GREGORIAN",
				"METHOD:PUBLISH",
				"X-WR-CALNAME:AgendaBeleza - Ana Souza",
				"REFRESH-INTERVAL;VALUE=DURATION:PT1H",
				"X-PUBLISHED-TTL:PT1H",
				"BEGIN:VEVENT",
				"UID:00000000-0000-0000-0000-0000000000a1@agendabeleza",
				"DTSTAMP:20300304T120000Z",
				"DTSTART:20300305T130000Z",
				"DTEND:20300305T134500Z",
				"SUMMARY:Corte feminino",
				"LOCATION:Rua Augusta\\, 100 - Centro",
				"DESCRIPTION:Serviço: Corte\\nPreço: R$ 80\\,00",
				"STATUS:CONFIRMED",
				"SEQUENCE:3",
				"END:VEVENT",
				"END:VCALENDAR",
				""));
	}

	@Test
	void agendamentoNaoConfirmadoSaiComoProvisorio() {
		String documento = gerar(evento(new DetalhesDoEvento("Corte", "Rua A", "Corte"), SituacaoDoEvento.PROVISORIO));

		assertThat(documento).contains(QUEBRA + "STATUS:TENTATIVE" + QUEBRA);
	}

	@Test
	void feedSemEventosTemSoOCalendario() {
		String documento = gerar();

		assertThat(documento).startsWith("BEGIN:VCALENDAR" + QUEBRA).endsWith("END:VCALENDAR" + QUEBRA)
				.doesNotContain("VEVENT");
	}

	@Test
	void escapaBarraPontoEVirgulaVirgulaEQuebrasDeLinha() {
		String documento = gerar(evento(new DetalhesDoEvento("A\\B; C, D", "Rua A", "linha 1\r\nlinha 2\nlinha 3"),
				SituacaoDoEvento.CONFIRMADO));

		assertThat(documento)
				.contains(QUEBRA + "SUMMARY:A\\\\B\\; C\\, D" + QUEBRA)
				.contains(QUEBRA + "DESCRIPTION:linha 1\\nlinha 2\\nlinha 3" + QUEBRA);
	}

	@Test
	void dobraLinhasLongasEm75OctetosSemPartirCaractereAcentuado() {
		String descricaoLonga = "Atenção: ".repeat(30);
		String documento = gerar(evento(new DetalhesDoEvento("Corte", "Rua A", descricaoLonga),
				SituacaoDoEvento.CONFIRMADO));

		String[] linhasFisicas = documento.split(QUEBRA);
		assertThat(linhasFisicas).allSatisfy(linha -> assertThat(linha.getBytes(StandardCharsets.UTF_8).length)
				.isLessThanOrEqualTo(LIMITE_DE_OCTETOS));
		assertThat(Arrays.stream(linhasFisicas).filter(linha -> linha.startsWith(" ")).count()).isGreaterThan(1);
		assertThat(desdobrar(documento)).contains(QUEBRA + "DESCRIPTION:" + descricaoLonga + QUEBRA);
	}

}
