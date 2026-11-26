package br.com.fiap.agendabeleza.api.notificacoes.domain;

/**
 * Situação de uma notificação na outbox (modelagem, seção 3.6). {@code PENDENTE} aguarda o job; {@code ENVIADA},
 * {@code FALHA} e {@code CANCELADA} são finais, e por isso o mesmo e-mail não é enviado de novo.
 */
public enum StatusNotificacao {

	PENDENTE,
	ENVIADA,
	FALHA,
	/** O motivo deixou de existir antes do envio, como o lembrete de um agendamento cancelado ou reagendado. */
	CANCELADA

}
