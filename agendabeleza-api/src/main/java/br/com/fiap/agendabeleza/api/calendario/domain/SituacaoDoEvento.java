package br.com.fiap.agendabeleza.api.calendario.domain;

import java.util.Optional;

import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;

/**
 * Como o agendamento aparece na agenda assinada. O adaptador traduz para o {@code STATUS} do iCalendar.
 */
public enum SituacaoDoEvento {

	/** Reservado e ainda não confirmado pelo estabelecimento. */
	PROVISORIO,
	CONFIRMADO;

	/**
	 * Cancelado não vira evento: sai do feed e, na atualização seguinte, some da agenda de quem assinou. Concluído e
	 * não comparecimento continuam no feed como histórico.
	 */
	public static Optional<SituacaoDoEvento> de(StatusAgendamento status) {
		return switch (status) {
			case AGENDADO -> Optional.of(PROVISORIO);
			case CONFIRMADO, CONCLUIDO, NAO_COMPARECEU -> Optional.of(CONFIRMADO);
			case CANCELADO -> Optional.empty();
		};
	}

}
