package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.compartilhado.domain.AcessoNegadoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.Intervalo;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.usecase.EstabelecimentoDoGestor;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;

/**
 * Período consultado no painel, em dias inteiros no fuso do estabelecimento. É a parte comum da agenda e do resumo:
 * carrega o estabelecimento, exige o gestor, aplica as datas padrão e limita o tamanho do período.
 *
 * @param de  primeiro dia, inclusivo
 * @param ate último dia, inclusivo
 */
public record PeriodoDoPainel(Estabelecimento estabelecimento, LocalDate de, LocalDate ate) {

	/** Limite para a consulta não varrer a agenda inteira do estabelecimento. */
	public static final int MAXIMO_DE_DIAS = 31;
	public static final String PERIODO_INVERTIDO = "A data final não pode ser anterior à data inicial";
	public static final String PERIODO_LONGO_DEMAIS = "O período do painel pode ter no máximo " + MAXIMO_DE_DIAS
			+ " dias";

	/**
	 * Sem {@code de}, usa {@code ate}; sem nenhuma das duas, usa hoje no fuso do estabelecimento. Sem {@code ate},
	 * o período é só o dia {@code de}.
	 *
	 * @throws RecursoNaoEncontradoException se o estabelecimento não existir
	 * @throws AcessoNegadoException         se o usuário não for o gestor
	 * @throws RegraNegocioException         se as datas estiverem invertidas ou o período passar de
	 *                                       {@link #MAXIMO_DE_DIAS}
	 */
	static PeriodoDoPainel carregar(EstabelecimentoRepository estabelecimentos, UUID estabelecimentoId,
			UUID usuarioId, LocalDate de, LocalDate ate, Clock relogio) {
		Estabelecimento estabelecimento = EstabelecimentoDoGestor.obter(estabelecimentos, estabelecimentoId,
				usuarioId, "consultar o painel");
		LocalDate hoje = LocalDate.now(relogio.withZone(estabelecimento.timezone()));
		LocalDate primeiroDia = primeiraDataInformada(de, ate, hoje);
		LocalDate ultimoDia = ate == null ? primeiroDia : ate;
		if (ultimoDia.isBefore(primeiroDia)) {
			throw new RegraNegocioException(PERIODO_INVERTIDO);
		}
		if (ChronoUnit.DAYS.between(primeiroDia, ultimoDia) >= MAXIMO_DE_DIAS) {
			throw new RegraNegocioException(PERIODO_LONGO_DEMAIS);
		}
		return new PeriodoDoPainel(estabelecimento, primeiroDia, ultimoDia);
	}

	private static LocalDate primeiraDataInformada(LocalDate de, LocalDate ate, LocalDate hoje) {
		if (de != null) {
			return de;
		}
		return ate != null ? ate : hoje;
	}

	public ZoneId fuso() {
		return estabelecimento.timezone();
	}

	/** Do início do primeiro dia ao início do dia seguinte ao último, no fuso do estabelecimento. */
	public Intervalo intervalo() {
		return new Intervalo(de.atStartOfDay(fuso()).toInstant(), ate.plusDays(1).atStartOfDay(fuso()).toInstant());
	}

}
