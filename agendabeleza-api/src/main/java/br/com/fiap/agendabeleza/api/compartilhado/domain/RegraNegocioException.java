package br.com.fiap.agendabeleza.api.compartilhado.domain;

/**
 * Violação de uma regra de negócio ou de um dado inválido. Traduzida para HTTP 422 pelos adaptadores web.
 */
public class RegraNegocioException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RegraNegocioException(String mensagem) {
		super(mensagem);
	}

}
