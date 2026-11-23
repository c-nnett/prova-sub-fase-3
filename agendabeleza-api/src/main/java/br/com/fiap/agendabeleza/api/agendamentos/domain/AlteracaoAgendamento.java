package br.com.fiap.agendabeleza.api.agendamentos.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da trilha {@code agendamento_historico}. O status anterior é nulo na reserva, e os inícios só são
 * preenchidos no reagendamento.
 */
public record AlteracaoAgendamento(
		StatusAgendamento statusAnterior,
		StatusAgendamento statusNovo,
		Instant inicioAnterior,
		Instant inicioNovo,
		String motivo,
		UUID alteradoPor,
		Instant ocorridoEm) {

	static AlteracaoAgendamento deStatus(StatusAgendamento anterior, StatusAgendamento novo, String motivo,
			UUID alteradoPor, Instant ocorridoEm) {
		return new AlteracaoAgendamento(anterior, novo, null, null, motivo, alteradoPor, ocorridoEm);
	}

}
