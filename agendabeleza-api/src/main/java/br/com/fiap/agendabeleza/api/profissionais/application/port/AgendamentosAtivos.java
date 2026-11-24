package br.com.fiap.agendabeleza.api.profissionais.application.port;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;

/**
 * Consulta aos agendamentos ativos de um profissional. A implementação fica no módulo de agendamentos, que já
 * depende de {@code profissionais}: a porta aqui evita que os dois módulos dependam um do outro.
 */
@FunctionalInterface
public interface AgendamentosAtivos {

	/** Indica se o profissional tem algum agendamento ativo que cruza o período. */
	boolean existemNoPeriodo(UUID profissionalId, Intervalo periodo);

}
