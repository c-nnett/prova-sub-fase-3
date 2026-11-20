package br.com.fiap.agendabeleza.api.compartilhado.domain;

/**
 * Recurso inexistente ou fora do escopo consultado. Traduzida para HTTP 404 pelos adaptadores web.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}
