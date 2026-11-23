package br.com.fiap.agendabeleza.api.compartilhado.domain;

/**
 * A operação conflita com o estado atual dos dados, como um horário já ocupado. Traduzida para HTTP 409 pelos
 * adaptadores web.
 */
public class ConflitoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ConflitoException(String mensagem) {
		super(mensagem);
	}

}
