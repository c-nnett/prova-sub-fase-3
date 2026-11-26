package br.com.fiap.agendabeleza.api.calendario.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;

/**
 * Um agendamento como evento da agenda assinada.
 *
 * @param sequencia versão do agendamento: aumenta a cada alteração, e o calendário usa isso para trocar o evento
 *                  em vez de duplicá-lo
 */
public record EventoDoCalendario(
		UUID agendamentoId,
		Instant inicio,
		Instant fim,
		DetalhesDoEvento detalhes,
		SituacaoDoEvento situacao,
		long sequencia) {

	/** Vazio para agendamento cancelado (ver {@link SituacaoDoEvento#de}). */
	public static Optional<EventoDoCalendario> doAgendamento(Agendamento agendamento, DetalhesDoEvento detalhes) {
		long sequencia = agendamento.versao() == null ? 0L : agendamento.versao();
		return SituacaoDoEvento.de(agendamento.status()).map(situacao -> new EventoDoCalendario(agendamento.id(),
				agendamento.inicio(), agendamento.fim(), detalhes, situacao, sequencia));
	}

}
