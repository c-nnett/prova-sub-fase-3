package br.com.fiap.agendabeleza.api.agendamentos.application.usecase;

import java.util.function.Function;

/**
 * Resultado do painel acompanhado do período resolvido, para a resposta informar as datas e o fuso usados.
 *
 * @param <T> a agenda ou o resumo
 */
public record DadosDoPeriodo<T>(PeriodoDoPainel periodo, T dados) {

	/** Deriva outro resultado do mesmo período, como o resumo a partir da agenda. */
	public <R> DadosDoPeriodo<R> transformar(Function<T, R> transformacao) {
		return new DadosDoPeriodo<>(periodo, transformacao.apply(dados));
	}

}
