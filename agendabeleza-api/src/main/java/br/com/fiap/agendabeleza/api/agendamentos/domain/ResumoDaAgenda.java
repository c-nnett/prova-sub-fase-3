package br.com.fiap.agendabeleza.api.agendamentos.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Totais do painel do estabelecimento para um conjunto de agendamentos: quantidade por status (todos os status
 * aparecem, com zero quando não houver), receita já realizada e receita ainda prevista.
 *
 * @param porStatus        mapa imutável, na ordem de {@link StatusAgendamento}
 * @param receitaRealizada soma dos preços dos agendamentos {@code CONCLUIDO}
 * @param receitaPrevista  soma dos preços dos agendamentos que ainda ocupam horário ({@code AGENDADO} e
 *                         {@code CONFIRMADO})
 */
public record ResumoDaAgenda(int total, Map<StatusAgendamento, Long> porStatus, BigDecimal receitaRealizada,
		BigDecimal receitaPrevista) {

	/** Duas casas, como a coluna {@code preco numeric(10,2)}. */
	private static final int CASAS_DECIMAIS = 2;

	public static ResumoDaAgenda de(List<Agendamento> agendamentos) {
		return new ResumoDaAgenda(agendamentos.size(), contarPorStatus(agendamentos),
				somarPrecos(agendamentos, agendamento -> agendamento.status() == StatusAgendamento.CONCLUIDO),
				somarPrecos(agendamentos, Agendamento::ocupaHorario));
	}

	private static Map<StatusAgendamento, Long> contarPorStatus(List<Agendamento> agendamentos) {
		Map<StatusAgendamento, Long> contagem = Arrays.stream(StatusAgendamento.values())
				.collect(Collectors.toMap(Function.identity(), status -> 0L, Long::sum,
						() -> new EnumMap<>(StatusAgendamento.class)));
		agendamentos.forEach(agendamento -> contagem.merge(agendamento.status(), 1L, Long::sum));
		return Collections.unmodifiableMap(contagem);
	}

	private static BigDecimal somarPrecos(List<Agendamento> agendamentos, Predicate<Agendamento> criterio) {
		return agendamentos.stream()
				.filter(criterio)
				.map(Agendamento::preco)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
	}

}
