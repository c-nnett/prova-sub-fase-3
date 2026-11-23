package br.com.fiap.agendabeleza.api.compartilhado.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class IntervaloTest {

	private static final Instant NOVE = Instant.parse("2026-11-30T12:00:00Z");
	private static final Instant DEZ = Instant.parse("2026-11-30T13:00:00Z");
	private static final Instant ONZE = Instant.parse("2026-11-30T14:00:00Z");
	private static final Instant MEIO_DIA = Instant.parse("2026-11-30T15:00:00Z");

	@Test
	void recusaFimAntesOuIgualAoInicio() {
		assertThatThrownBy(() -> new Intervalo(DEZ, DEZ))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("fim deve ser posterior a inicio");
		assertThatThrownBy(() -> new Intervalo(DEZ, NOVE))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("fim deve ser posterior a inicio");
	}

	@Test
	void recusaCamposAusentes() {
		assertThatThrownBy(() -> new Intervalo(null, DEZ)).hasMessage("inicio é obrigatório");
		assertThatThrownBy(() -> new Intervalo(NOVE, null)).hasMessage("fim é obrigatório");
	}

	@Test
	void sobrepoeIntervaloQueSeCruza() {
		Intervalo primeiro = new Intervalo(NOVE, ONZE);
		Intervalo segundo = new Intervalo(DEZ, MEIO_DIA);

		assertThat(primeiro.sobrepoe(segundo)).isTrue();
		assertThat(segundo.sobrepoe(primeiro)).isTrue();
	}

	@Test
	void naoSobrepoeIntervaloQueSoEncosta() {
		Intervalo primeiro = new Intervalo(NOVE, DEZ);
		Intervalo segundo = new Intervalo(DEZ, ONZE);

		assertThat(primeiro.sobrepoe(segundo)).isFalse();
		assertThat(segundo.sobrepoe(primeiro)).isFalse();
	}

	@Test
	void naoSobrepoeIntervaloDistante() {
		assertThat(new Intervalo(NOVE, DEZ).sobrepoe(new Intervalo(ONZE, MEIO_DIA))).isFalse();
	}

	@Test
	void contemIntervaloInternoOuIgual() {
		Intervalo manha = new Intervalo(NOVE, MEIO_DIA);

		assertThat(manha.contem(new Intervalo(DEZ, ONZE))).isTrue();
		assertThat(manha.contem(new Intervalo(NOVE, MEIO_DIA))).isTrue();
		assertThat(manha.contem(new Intervalo(DEZ, MEIO_DIA.plusSeconds(1)))).isFalse();
		assertThat(manha.contem(new Intervalo(NOVE.minusSeconds(1), DEZ))).isFalse();
	}

	@Test
	void calculaDuracao() {
		assertThat(new Intervalo(NOVE, DEZ).duracao()).hasMinutes(60);
	}

}
