package br.com.fiap.agendabeleza.api.notificacoes.domain;

/**
 * Quem recebe a notificação. Identifica o e-mail do cliente, que só existe na outbox, para os avisos seguintes do
 * mesmo agendamento.
 */
public enum PapelDestinatario {

	CLIENTE,
	PROFISSIONAL

}
