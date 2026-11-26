package br.com.fiap.agendabeleza.api.compartilhado.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Nota média e total de avaliações de um estabelecimento ou profissional, gravados em {@code nota_media numeric(3,2)}
 * e {@code total_avaliacoes}.
 * <p>
 * É sempre calculada a partir da soma de todas as notas, e não de forma incremental sobre a média anterior: a média
 * gravada tem duas casas, e acumular sobre ela acumularia o arredondamento.
 *
 * @param media nula enquanto não houver avaliação
 */
public record NotaMedia(BigDecimal media, int total) {

	public static final NotaMedia SEM_AVALIACOES = new NotaMedia(null, 0);

	private static final int CASAS_DECIMAIS = 2;

	public static NotaMedia de(long somaDasNotas, int total) {
		if (total < 0) {
			throw new IllegalArgumentException("total de avaliações negativo: " + total);
		}
		if (total == 0) {
			return SEM_AVALIACOES;
		}
		BigDecimal media = BigDecimal.valueOf(somaDasNotas)
				.divide(BigDecimal.valueOf(total), CASAS_DECIMAIS, RoundingMode.HALF_UP);
		return new NotaMedia(media, total);
	}

}
