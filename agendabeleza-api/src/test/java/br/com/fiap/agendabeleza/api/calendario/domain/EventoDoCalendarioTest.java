package br.com.fiap.agendabeleza.api.calendario.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;

class EventoDoCalendarioTest {

	private static final Instant INICIO = Instant.parse("2030-03-05T13:00:00Z");
	private static final Instant FIM = Instant.parse("2030-03-05T13:45:00Z");
	private static final DetalhesDoEvento DETALHES = new DetalhesDoEvento("Corte em Salão Bela",
			"Rua A, 10 - Centro, São Paulo/SP", "Profissional: Ana");

	private static Agendamento agendamento(StatusAgendamento status) {
		return Agendamento.reconstituir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), INICIO, FIM, status, new BigDecimal("80.00"), null, 3L);
	}

	@ParameterizedTest
	@CsvSource({ "AGENDADO, PROVISORIO", "CONFIRMADO, CONFIRMADO", "CONCLUIDO, CONFIRMADO",
			"NAO_COMPARECEU, CONFIRMADO" })
	void agendamentoQueNaoFoiCanceladoViraEvento(StatusAgendamento status, SituacaoDoEvento situacaoEsperada) {
		Agendamento agendamento = agendamento(status);

		EventoDoCalendario evento = EventoDoCalendario.doAgendamento(agendamento, DETALHES).orElseThrow();

		assertThat(evento.agendamentoId()).isEqualTo(agendamento.id());
		assertThat(evento.inicio()).isEqualTo(INICIO);
		assertThat(evento.fim()).isEqualTo(FIM);
		assertThat(evento.detalhes()).isEqualTo(DETALHES);
		assertThat(evento.situacao()).isEqualTo(situacaoEsperada);
		assertThat(evento.sequencia()).isEqualTo(3L);
	}

	@Test
	void agendamentoCanceladoSaiDoCalendario() {
		assertThat(EventoDoCalendario.doAgendamento(agendamento(StatusAgendamento.CANCELADO), DETALHES)).isEmpty();
	}

	@Test
	void agendamentoSemVersaoComecaNaSequenciaZero() {
		Agendamento novo = Agendamento.reconstituir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), INICIO, FIM, StatusAgendamento.AGENDADO, new BigDecimal("80.00"), null, null);

		assertThat(EventoDoCalendario.doAgendamento(novo, DETALHES).orElseThrow().sequencia()).isZero();
	}

}
