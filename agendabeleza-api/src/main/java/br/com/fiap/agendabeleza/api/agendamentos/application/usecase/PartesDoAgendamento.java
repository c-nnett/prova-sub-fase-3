package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.util.Objects;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;

/**
 * O agendamento com o estabelecimento e o profissional dele, carregados uma única vez. Responde quem é quem para as
 * regras de permissão das consultas e das ações de gestão.
 */
record PartesDoAgendamento(Agendamento agendamento, Estabelecimento estabelecimento, Profissional profissional) {

	/** @throws RecursoNaoEncontradoException se o agendamento não existir */
	static PartesDoAgendamento carregar(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, UUID agendamentoId) {
		Agendamento agendamento = agendamentos.buscarPorId(agendamentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Agendamento " + agendamentoId + " não encontrado"));
		Estabelecimento estabelecimento = estabelecimentos.buscarPorId(agendamento.estabelecimentoId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Estabelecimento " + agendamento.estabelecimentoId() + " não encontrado"));
		Profissional profissional = profissionais.buscarPorId(agendamento.profissionalId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Profissional " + agendamento.profissionalId() + " não encontrado"));
		return new PartesDoAgendamento(agendamento, estabelecimento, profissional);
	}

	boolean ehCliente(UUID usuario) {
		return agendamento.pertenceAoCliente(usuario);
	}

	boolean ehGestor(UUID usuario) {
		return estabelecimento.gestorId().equals(usuario);
	}

	/** O profissional pode não ter login ({@code usuarioId} nulo); nesse caso ninguém age como ele. */
	boolean ehProfissional(UUID usuario) {
		return Objects.equals(profissional.usuarioId(), usuario);
	}

}
