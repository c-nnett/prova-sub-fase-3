package br.com.fiap.agendabeleza.api.agendamentos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StatusAgendamentoTest {

	/** Transições do diagrama 4.2 da modelagem de dados. Qualquer outro par é inválido. */
	private static final Set<String> TRANSICOES_DO_DIAGRAMA = Set.of(
			"AGENDADO>CONFIRMADO",
			"AGENDADO>AGENDADO",
			"CONFIRMADO>AGENDADO",
			"AGENDADO>CANCELADO",
			"CONFIRMADO>CANCELADO",
			"CONFIRMADO>CONCLUIDO",
			"CONFIRMADO>NAO_COMPARECEU");

	static Stream<Arguments> todosOsPares() {
		return Arrays.stream(StatusAgendamento.values())
				.flatMap(origem -> Arrays.stream(StatusAgendamento.values())
						.map(destino -> Arguments.of(origem, destino,
								TRANSICOES_DO_DIAGRAMA.contains(origem + ">" + destino))));
	}

	@ParameterizedTest(name = "{0} -> {1}: {2}")
	@MethodSource("todosOsPares")
	void segueODiagramaDeStatus(StatusAgendamento origem, StatusAgendamento destino, boolean permitida) {
		assertThat(origem.podeIrPara(destino)).isEqualTo(permitida);
	}

	@Test
	void naoPermiteDestinoNulo() {
		assertThat(StatusAgendamento.AGENDADO.podeIrPara(null)).isFalse();
	}

	@Test
	void somenteAgendadoEConfirmadoOcupamOHorario() {
		assertThat(StatusAgendamento.AGENDADO.ocupaHorario()).isTrue();
		assertThat(StatusAgendamento.CONFIRMADO.ocupaHorario()).isTrue();
		assertThat(StatusAgendamento.CONCLUIDO.ocupaHorario()).isFalse();
		assertThat(StatusAgendamento.CANCELADO.ocupaHorario()).isFalse();
		assertThat(StatusAgendamento.NAO_COMPARECEU.ocupaHorario()).isFalse();
	}

	@Test
	void estadosFinaisNaoTemSaida() {
		assertThat(StatusAgendamento.CONCLUIDO.finalizado()).isTrue();
		assertThat(StatusAgendamento.CANCELADO.finalizado()).isTrue();
		assertThat(StatusAgendamento.NAO_COMPARECEU.finalizado()).isTrue();
		assertThat(StatusAgendamento.AGENDADO.finalizado()).isFalse();
		assertThat(StatusAgendamento.CONFIRMADO.finalizado()).isFalse();
	}

}
