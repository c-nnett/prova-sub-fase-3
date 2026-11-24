package br.com.fiap.agendabeleza.api.agendamentos.domain;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

/**
 * Operação fora do diagrama de status do agendamento. Por ser uma {@link RegraNegocioException}, vira HTTP 422.
 * O 409 fica reservado para o conflito de horário.
 */
public class TransicaoInvalidaException extends RegraNegocioException {

	private static final long serialVersionUID = 1L;

	private final StatusAgendamento statusAtual;

	/**
	 * @param acao verbo da operação recusada, completando a frase "Não é possível ... um agendamento STATUS"
	 */
	public TransicaoInvalidaException(String acao, StatusAgendamento statusAtual) {
		super("Não é possível " + acao + " um agendamento " + statusAtual);
		this.statusAtual = statusAtual;
	}

	public StatusAgendamento statusAtual() {
		return statusAtual;
	}

}
