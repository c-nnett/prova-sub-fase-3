package br.com.fiap.agendabeleza.api.agendamentos.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.HorarioIndisponivelException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;

/**
 * Porta de persistência dos agendamentos e da trilha de alterações.
 */
public interface AgendamentoRepository {

	/**
	 * Grava o agendamento e as alterações pendentes na mesma transação.
	 *
	 * @return o agendamento como ficou gravado, com a versão atualizada e sem alterações pendentes
	 * @throws HorarioIndisponivelException se o profissional já tiver um agendamento ativo sobreposto
	 */
	Agendamento salvar(Agendamento agendamento);

	Optional<Agendamento> buscarPorId(UUID id);

	/** Intervalos dos agendamentos ativos do profissional que cruzam o período, em ordem de início. */
	List<Intervalo> ocupacoesDoProfissional(UUID profissionalId, Intervalo periodo);

	/** Agendamentos do cliente, do início mais recente para o mais antigo. */
	List<Agendamento> listarDoCliente(UUID clienteId);

}
