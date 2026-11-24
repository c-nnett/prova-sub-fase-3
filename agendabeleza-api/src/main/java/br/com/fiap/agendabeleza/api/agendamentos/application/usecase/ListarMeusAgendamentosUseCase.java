package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.util.List;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;

/**
 * Agendamentos em que o usuário é o cliente, do início mais recente para o mais antigo.
 */
public class ListarMeusAgendamentosUseCase {

	private final AgendamentoRepository agendamentos;

	public ListarMeusAgendamentosUseCase(AgendamentoRepository agendamentos) {
		this.agendamentos = agendamentos;
	}

	public List<Agendamento> executar(UUID clienteId) {
		return agendamentos.listarDoCliente(clienteId);
	}

}
