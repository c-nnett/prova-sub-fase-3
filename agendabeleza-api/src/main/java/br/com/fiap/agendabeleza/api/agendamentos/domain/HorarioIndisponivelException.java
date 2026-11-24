package br.com.fiap.agendabeleza.api.agendamentos.domain;

import br.com.fiap.agendabeleza.api.compartilhado.domain.ConflitoException;

/**
 * O profissional já tem um agendamento ativo que se sobrepõe ao horário pedido. No banco, corresponde à violação
 * da restrição {@code agendamento_sem_conflito}.
 */
public class HorarioIndisponivelException extends ConflitoException {

	private static final long serialVersionUID = 1L;

	public HorarioIndisponivelException() {
		super("O profissional já tem um agendamento nesse horário");
	}

}
