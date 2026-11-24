package br.com.fiap.agendabeleza.api.agendamentos.domain;

/**
 * Status do agendamento e as transições permitidas pelo diagrama 4.2 da modelagem de dados.
 * <p>
 * {@code AGENDADO -> AGENDADO} e {@code CONFIRMADO -> AGENDADO} representam o reagendamento.
 */
public enum StatusAgendamento {

	AGENDADO,
	CONFIRMADO,
	CONCLUIDO,
	CANCELADO,
	NAO_COMPARECEU;

	public boolean podeIrPara(StatusAgendamento destino) {
		if (destino == null) {
			return false;
		}
		return switch (this) {
			case AGENDADO -> destino == AGENDADO || destino == CONFIRMADO || destino == CANCELADO;
			case CONFIRMADO -> destino == AGENDADO || destino == CANCELADO || destino == CONCLUIDO
					|| destino == NAO_COMPARECEU;
			case CONCLUIDO, CANCELADO, NAO_COMPARECEU -> false;
		};
	}

	/** Mesma condição da restrição {@code agendamento_sem_conflito}: só agendamentos ativos bloqueiam o horário. */
	public boolean ocupaHorario() {
		return this == AGENDADO || this == CONFIRMADO;
	}

	public boolean finalizado() {
		return !ocupaHorario();
	}

}
