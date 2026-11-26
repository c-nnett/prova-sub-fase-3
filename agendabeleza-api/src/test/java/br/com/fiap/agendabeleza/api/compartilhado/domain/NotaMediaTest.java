package br.com.fiap.agendabeleza.api.compartilhado.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NotaMediaTest {

	@Test
	void semAvaliacoesAMediaEhNula() {
		NotaMedia notas = NotaMedia.de(0, 0);

		assertThat(notas.media()).isNull();
		assertThat(notas.total()).isZero();
		assertThat(notas).isEqualTo(NotaMedia.SEM_AVALIACOES);
	}

	@ParameterizedTest(name = "soma {0} em {1} avaliações → {2}")
	@CsvSource({
			"5, 1, 5.00",
			"9, 2, 4.50",
			"13, 3, 4.33",
			"14, 3, 4.67",
			"10, 6, 1.67" })
	void mediaComDuasCasasArredondandoMeioParaCima(long somaDasNotas, int total, String mediaEsperada) {
		NotaMedia notas = NotaMedia.de(somaDasNotas, total);

		assertThat(notas.media()).isEqualTo(new BigDecimal(mediaEsperada));
		assertThat(notas.total()).isEqualTo(total);
	}

	@Test
	void recusaTotalNegativo() {
		assertThatThrownBy(() -> NotaMedia.de(5, -1))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
