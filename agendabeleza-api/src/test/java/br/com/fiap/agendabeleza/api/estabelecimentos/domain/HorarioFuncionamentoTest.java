package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class HorarioFuncionamentoTest {

	private final HorarioFuncionamento segunda = new HorarioFuncionamento(
			DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0));

	@Test
	void recusaFechamentoAntesOuIgualAAbertura() {
		assertThatThrownBy(() -> new HorarioFuncionamento(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(18, 0)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("fechaAs deve ser posterior a abreAs");
	}

	@Test
	void recusaCamposAusentes() {
		assertThatThrownBy(() -> new HorarioFuncionamento(null, LocalTime.of(9, 0), LocalTime.of(18, 0)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("diaSemana é obrigatório");
	}

	@Test
	void abrangeIntervaloDentroDoFuncionamento() {
		assertThat(segunda.abrange(LocalTime.of(9, 0), LocalTime.of(18, 0))).isTrue();
		assertThat(segunda.abrange(LocalTime.of(17, 0), LocalTime.of(18, 0))).isTrue();
	}

	@Test
	void naoAbrangeIntervaloForaDoFuncionamento() {
		assertThat(segunda.abrange(LocalTime.of(8, 30), LocalTime.of(9, 30))).isFalse();
		assertThat(segunda.abrange(LocalTime.of(17, 30), LocalTime.of(18, 30))).isFalse();
	}

}
