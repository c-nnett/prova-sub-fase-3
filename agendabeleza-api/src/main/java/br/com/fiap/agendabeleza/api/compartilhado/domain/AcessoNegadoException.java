package br.com.fiap.agendabeleza.api.compartilhado.domain;

/**
 * O usuário identificado não tem permissão para a operação. Traduzida para HTTP 403 pelos adaptadores web.
 */
public class AcessoNegadoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AcessoNegadoException(String mensagem) {
		super(mensagem);
	}

}
