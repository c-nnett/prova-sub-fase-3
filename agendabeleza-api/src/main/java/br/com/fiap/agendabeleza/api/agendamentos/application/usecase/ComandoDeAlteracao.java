package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.util.UUID;

/**
 * Entrada comum das ações de gestão sobre um agendamento existente: qual agendamento e quem pede a alteração.
 * Cada ação que precisa de dados extras declara o próprio comando implementando esta interface.
 */
public interface ComandoDeAlteracao {

	UUID agendamentoId();

	UUID usuarioSolicitante();

	/** Comando das ações que não recebem dados além do agendamento e do solicitante. */
	record Simples(UUID agendamentoId, UUID usuarioSolicitante) implements ComandoDeAlteracao {
	}

}
