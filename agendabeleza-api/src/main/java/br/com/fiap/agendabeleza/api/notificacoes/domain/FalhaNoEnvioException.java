package br.com.fiap.agendabeleza.api.notificacoes.domain;

/**
 * O servidor de e-mail não aceitou a mensagem. Não é erro do cliente da API: quem envia decide se tenta de novo,
 * e a reserva que originou o e-mail continua válida.
 */
public class FalhaNoEnvioException extends RuntimeException {

	public FalhaNoEnvioException(String destinatario, Throwable causa) {
		super("Falha ao enviar e-mail para " + destinatario, causa);
	}

}
