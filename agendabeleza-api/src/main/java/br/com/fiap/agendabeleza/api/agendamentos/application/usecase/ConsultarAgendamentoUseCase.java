package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.util.Objects;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Detalhes de um agendamento para as partes envolvidas: o cliente, o gestor do estabelecimento e o profissional.
 */
public class ConsultarAgendamentoUseCase {

	private final AgendamentoRepository agendamentos;
	private final ProfissionalRepository profissionais;
	private final EstabelecimentoRepository estabelecimentos;

	public ConsultarAgendamentoUseCase(AgendamentoRepository agendamentos, ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos) {
		this.agendamentos = agendamentos;
		this.profissionais = profissionais;
		this.estabelecimentos = estabelecimentos;
	}

	public Agendamento executar(UUID agendamentoId, UUID usuarioId) {
		Agendamento agendamento = agendamentos.buscarPorId(agendamentoId)
				.orElseThrow(() -> new RecursoNaoEncontradoException(
						"Agendamento " + agendamentoId + " não encontrado"));
		if (!envolvido(agendamento, usuarioId)) {
			throw new AcessoNegadoException("Somente o cliente, o gestor do estabelecimento ou o profissional "
					+ "podem consultar este agendamento");
		}
		return agendamento;
	}

	private boolean envolvido(Agendamento agendamento, UUID usuarioId) {
		return agendamento.pertenceAoCliente(usuarioId)
				|| estabelecimentos.buscarPorId(agendamento.estabelecimentoId())
						.map(estabelecimento -> estabelecimento.gestorId().equals(usuarioId))
						.orElse(false)
				|| profissionais.buscarPorId(agendamento.profissionalId())
						.map(profissional -> Objects.equals(profissional.usuarioId(), usuarioId))
						.orElse(false);
	}

}
