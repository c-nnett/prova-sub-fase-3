package br.com.fiap.agendabeleza.api.agendamentos.adapters.out.persistence;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.FiltroDaAgenda;
import br.com.fiap.agendabeleza.api.agendamentos.domain.StatusAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;

/**
 * Critérios combináveis da agenda do painel. Filtro opcional ausente vira {@link Specification#unrestricted()}, em vez
 * de JPQL com {@code :parametro is null}, que o PostgreSQL recusa para UUID nulo por não conseguir inferir o tipo.
 */
final class AgendamentoEspecificacoes {

	private AgendamentoEspecificacoes() {
	}

	static Specification<AgendamentoJpaEntity> da(FiltroDaAgenda filtro) {
		return Specification.allOf(
				doEstabelecimento(filtro.estabelecimentoId()),
				iniciandoEm(filtro.periodo()),
				comStatus(filtro.status()),
				doProfissional(filtro.profissionalId()));
	}

	static Specification<AgendamentoJpaEntity> doEstabelecimento(UUID estabelecimentoId) {
		return (raiz, consulta, criterios) -> criterios.equal(raiz.get("estabelecimentoId"), estabelecimentoId);
	}

	/** Início em {@code [inicio, fim)}, como o índice {@code idx_agendamento_estab_inicio}. */
	static Specification<AgendamentoJpaEntity> iniciandoEm(Intervalo periodo) {
		return (raiz, consulta, criterios) -> criterios.and(
				criterios.greaterThanOrEqualTo(raiz.get("inicio"), periodo.inicio()),
				criterios.lessThan(raiz.get("inicio"), periodo.fim()));
	}

	static Specification<AgendamentoJpaEntity> comStatus(Set<StatusAgendamento> status) {
		if (status.isEmpty()) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, criterios) -> raiz.get("status").in(status);
	}

	static Specification<AgendamentoJpaEntity> doProfissional(UUID profissionalId) {
		if (profissionalId == null) {
			return Specification.unrestricted();
		}
		return (raiz, consulta, criterios) -> criterios.equal(raiz.get("profissionalId"), profissionalId);
	}

}
