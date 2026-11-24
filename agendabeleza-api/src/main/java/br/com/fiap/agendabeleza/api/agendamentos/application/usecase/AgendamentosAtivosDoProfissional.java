package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.profissionais.application.port.AgendamentosAtivos;

/**
 * Implementa a porta {@link AgendamentosAtivos} do módulo de profissionais com os agendamentos ativos gravados.
 * Usada para recusar um bloqueio de agenda sobre um agendamento.
 */
public class AgendamentosAtivosDoProfissional implements AgendamentosAtivos {

	private final AgendamentoRepository agendamentos;

	public AgendamentosAtivosDoProfissional(AgendamentoRepository agendamentos) {
		this.agendamentos = agendamentos;
	}

	@Override
	public boolean existemNoPeriodo(UUID profissionalId, Intervalo periodo) {
		return !agendamentos.ocupacoesDoProfissional(profissionalId, periodo).isEmpty();
	}

}
