package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.ResumoDaAgenda;

/**
 * Resumo do painel: totais por status e receitas da mesma agenda que o gestor consulta. Reaproveita a consulta da
 * agenda, com as mesmas regras de permissão e de período, sem filtrar por status.
 */
public class ResumirAgendaDoEstabelecimentoUseCase {

	private final ConsultarAgendaDoEstabelecimentoUseCase consultarAgenda;

	public ResumirAgendaDoEstabelecimentoUseCase(ConsultarAgendaDoEstabelecimentoUseCase consultarAgenda) {
		this.consultarAgenda = consultarAgenda;
	}

	public DadosDoPeriodo<ResumoDaAgenda> executar(Consulta consulta) {
		return consultarAgenda.executar(new ConsultarAgendaDoEstabelecimentoUseCase.Consulta(
				consulta.estabelecimentoId(), consulta.usuarioId(), consulta.de(), consulta.ate(), Set.of(),
				consulta.profissionalId()))
				.transformar(ResumoDaAgenda::de);
	}

	/** Mesmo significado dos campos de {@link ConsultarAgendaDoEstabelecimentoUseCase.Consulta}. */
	public record Consulta(UUID estabelecimentoId, UUID usuarioId, LocalDate de, LocalDate ate,
			UUID profissionalId) {
	}

}
