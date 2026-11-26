package br.com.fiap.agendabeleza.api.busca.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class JanelaDeDisponibilidadeTest {

	private static final LocalDate HOJE = LocalDate.of(2030, 3, 4);
	private static final LocalDate SEXTA = LocalDate.of(2030, 3, 8);

	@Test
	void semDataNemHorasNaoFiltraPorDisponibilidade() {
		assertThat(JanelaDeDisponibilidade.de(null, null, null, HOJE)).isNull();
	}

	@Test
	void soComADataValeODiaInteiro() {
		JanelaDeDisponibilidade janela = JanelaDeDisponibilidade.de(SEXTA, null, null, HOJE);

		assertThat(janela.data()).isEqualTo(SEXTA);
		assertThat(janela.inicio()).isEqualTo(LocalTime.MIDNIGHT);
		assertThat(janela.fim()).isNull();
	}

	@Test
	void guardaAsHorasInformadas() {
		JanelaDeDisponibilidade janela = JanelaDeDisponibilidade.de(SEXTA, LocalTime.of(14, 0), LocalTime.of(18, 0),
				HOJE);

		assertThat(janela.inicio()).isEqualTo(LocalTime.of(14, 0));
		assertThat(janela.fim()).isEqualTo(LocalTime.of(18, 0));
	}

	@Test
	void soComOFimComecaNaMeiaNoite() {
		JanelaDeDisponibilidade janela = JanelaDeDisponibilidade.de(SEXTA, null, LocalTime.of(12, 0), HOJE);

		assertThat(janela.inicio()).isEqualTo(LocalTime.MIDNIGHT);
		assertThat(janela.fim()).isEqualTo(LocalTime.of(12, 0));
	}

	@Test
	void diaDaSemanaSegueONumeroIsoDasColunas() {
		JanelaDeDisponibilidade janela = JanelaDeDisponibilidade.de(SEXTA, null, null, HOJE);

		assertThat(SEXTA.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
		assertThat(janela.diaDaSemana()).isEqualTo(5);
	}

	@Test
	void aceitaODiaDeHoje() {
		assertThat(JanelaDeDisponibilidade.de(HOJE, null, null, HOJE).data()).isEqualTo(HOJE);
	}

	@Test
	void recusaDataNoPassado() {
		LocalDate ontem = HOJE.minusDays(1);

		assertThatThrownBy(() -> JanelaDeDisponibilidade.de(ontem, null, null, HOJE))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(JanelaDeDisponibilidade.DATA_NO_PASSADO);
	}

	@ParameterizedTest(name = "início {0}, fim {1}")
	@CsvSource(value = { "14:00, NULL", "NULL, 18:00", "14:00, 18:00" }, nullValues = "NULL")
	void recusaHoraSemData(LocalTime inicio, LocalTime fim) {
		assertThatThrownBy(() -> JanelaDeDisponibilidade.de(null, inicio, fim, HOJE))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(JanelaDeDisponibilidade.HORA_SEM_DATA);
	}

	@ParameterizedTest(name = "início {0}, fim {1}")
	@CsvSource({ "14:00, 14:00", "18:00, 14:00", "00:00, 00:00" })
	void recusaJanelaVaziaOuInvertida(LocalTime inicio, LocalTime fim) {
		assertThatThrownBy(() -> JanelaDeDisponibilidade.de(SEXTA, inicio, fim, HOJE))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(JanelaDeDisponibilidade.JANELA_INVERTIDA);
	}

}
