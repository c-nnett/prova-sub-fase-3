package br.com.fiap.agendabeleza.api.agendamentos.application.port;

import java.util.Set;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;

/**
 * Critérios da agenda do painel. Um agendamento entra no período se o início cair em {@code [inicio, fim)}.
 *
 * @param status         vazio (ou nulo) significa todos os status
 * @param profissionalId nulo significa todos os profissionais
 */
public record FiltroDaAgenda(UUID estabelecimentoId, Intervalo periodo, Set<StatusAgendamento> status,
		UUID profissionalId) {

	public FiltroDaAgenda {
		status = status == null ? Set.of() : Set.copyOf(status);
	}

}
