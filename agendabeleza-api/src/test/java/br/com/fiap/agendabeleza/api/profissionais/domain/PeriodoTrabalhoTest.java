package br.com.fiap.agendabeleza.api.profissionais.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class PeriodoTrabalhoTest {

	private final PeriodoTrabalho manha = new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

	@Test
	void recusaFimAntesOuIgualAoInicio() {
		assertThatThrownBy(() -> new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(12, 0)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("fim deve ser posterior a inicio");
	}

	@Test
	void recusaCamposAusentes() {
		assertThatThrownBy(() -> new PeriodoTrabalho(null, LocalTime.of(8, 0), LocalTime.of(12, 0)))
				.hasMessage("diaSemana é obrigatório");
		assertThatThrownBy(() -> new PeriodoTrabalho(DayOfWeek.MONDAY, null, LocalTime.of(12, 0)))
				.hasMessage("inicio é obrigatório");
		assertThatThrownBy(() -> new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(8, 0), null))
				.hasMessage("fim é obrigatório");
	}

	@Test
	void sobrepoePeriodoDoMesmoDiaQueSeCruza() {
		PeriodoTrabalho cruzando = new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0));

		assertThat(manha.sobrepoe(cruzando)).isTrue();
		assertThat(cruzando.sobrepoe(manha)).isTrue();
	}

	@Test
	void naoSobrepoePeriodoQueSoEncosta() {
		PeriodoTrabalho tarde = new PeriodoTrabalho(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(18, 0));

		assertThat(manha.sobrepoe(tarde)).isFalse();
		assertThat(tarde.sobrepoe(manha)).isFalse();
	}

	@Test
	void naoSobrepoePeriodoDeOutroDia() {
		PeriodoTrabalho terca = new PeriodoTrabalho(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

		assertThat(manha.sobrepoe(terca)).isFalse();
	}

}
