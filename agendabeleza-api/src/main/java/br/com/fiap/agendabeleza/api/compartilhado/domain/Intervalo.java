package br.com.fiap.agendabeleza.api.compartilhado.domain;

import static br.com.fiap.agendabeleza.api.compartilhado.domain.Validacoes.obrigatorio;

import java.time.Duration;
import java.time.Instant;

/**
 * Intervalo de tempo semiaberto {@code [inicio, fim)}, como o {@code tstzrange '[)'} do banco. Um intervalo pode
 * começar no instante exato em que outro termina sem que os dois se sobreponham.
 */
public record Intervalo(Instant inicio, Instant fim) {

	public Intervalo {
		obrigatorio(inicio, "inicio");
		obrigatorio(fim, "fim");
		if (!fim.isAfter(inicio)) {
			throw new RegraNegocioException("fim deve ser posterior a inicio");
		}
	}

	/** Indica se os dois intervalos se cruzam. Intervalos que só se encostam não se sobrepõem. */
	public boolean sobrepoe(Intervalo outro) {
		return inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
	}

	/** Indica se o outro intervalo cabe inteiro neste. */
	public boolean contem(Intervalo outro) {
		return !outro.inicio.isBefore(inicio) && !outro.fim.isAfter(fim);
	}

	public Duration duracao() {
		return Duration.between(inicio, fim);
	}

}
