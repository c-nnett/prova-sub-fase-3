package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AgendamentoRepository;
import br.com.fiap.agendabeleza.api.agendamentos.application.port.FiltroDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;

/**
 * Painel do estabelecimento: agenda do período filtrada por status e profissional, em ordem de início. Somente o
 * gestor consulta (funcionalidade 6).
 */
public class ConsultarAgendaDoEstabelecimentoUseCase {

	private final EstabelecimentoRepository estabelecimentos;
	private final AgendamentoRepository agendamentos;
	private final Clock relogio;

	public ConsultarAgendaDoEstabelecimentoUseCase(EstabelecimentoRepository estabelecimentos,
			AgendamentoRepository agendamentos, Clock relogio) {
		this.estabelecimentos = estabelecimentos;
		this.agendamentos = agendamentos;
		this.relogio = relogio;
	}

	public DadosDoPeriodo<List<Agendamento>> executar(Consulta consulta) {
		PeriodoDoPainel periodo = PeriodoDoPainel.carregar(estabelecimentos, consulta.estabelecimentoId(),
				consulta.usuarioId(), consulta.de(), consulta.ate(), relogio);
		List<Agendamento> agenda = agendamentos.listarDoEstabelecimento(new FiltroDaAgenda(
				consulta.estabelecimentoId(), periodo.intervalo(), consulta.status(), consulta.profissionalId()));
		return new DadosDoPeriodo<>(periodo, agenda);
	}

	/**
	 * @param de             primeiro dia no fuso do estabelecimento; nulo usa {@code ate} ou hoje
	 * @param ate            último dia, inclusivo; nulo usa {@code de}
	 * @param status         vazio ou nulo significa todos
	 * @param profissionalId nulo significa todos
	 */
	public record Consulta(UUID estabelecimentoId, UUID usuarioId, LocalDate de, LocalDate ate,
			Set<StatusAgendamento> status, UUID profissionalId) {
	}

}
