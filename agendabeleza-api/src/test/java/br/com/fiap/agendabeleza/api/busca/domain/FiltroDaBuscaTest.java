package br.com.fiap.agendabeleza.api.busca.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.CategoriaServico;

class FiltroDaBuscaTest {

	private static FiltroDaBusca comPrecos(String precoMinimo, String precoMaximo) {
		return new FiltroDaBusca(null, null, null, null, null, null, decimal(precoMinimo), decimal(precoMaximo),
				null);
	}

	private static FiltroDaBusca comNotaMinima(String notaMinima) {
		return new FiltroDaBusca(null, null, null, null, null, decimal(notaMinima), null, null, null);
	}

	private static BigDecimal decimal(String valor) {
		return valor == null ? null : new BigDecimal(valor);
	}

	@Test
	void removeEspacosDasPontasDosTextos() {
		FiltroDaBusca filtro = new FiltroDaBusca("  Bela Vista ", " São Paulo", "Consolação ", " corte ", null, null,
				null, null, null);

		assertThat(filtro.nome()).isEqualTo("Bela Vista");
		assertThat(filtro.cidade()).isEqualTo("São Paulo");
		assertThat(filtro.bairro()).isEqualTo("Consolação");
		assertThat(filtro.servico()).isEqualTo("corte");
	}

	@Test
	void textoEmBrancoNaoFiltra() {
		FiltroDaBusca filtro = new FiltroDaBusca(" ", "", "\t", "  ", null, null, null, null, null);

		assertThat(filtro.nome()).isNull();
		assertThat(filtro.cidade()).isNull();
		assertThat(filtro.bairro()).isNull();
		assertThat(filtro.servico()).isNull();
	}

	@Test
	void semCriteriosDeServicoNaoFiltraPorServico() {
		FiltroDaBusca filtro = new FiltroDaBusca("Bela", "São Paulo", null, null, null, new BigDecimal("4"), null,
				null, null);

		assertThat(filtro.filtraPorServico()).isFalse();
	}

	@Test
	void nomeDoServicoCategoriaOuPrecoFiltramPorServico() {
		assertThat(new FiltroDaBusca(null, null, null, "corte", null, null, null, null, null).filtraPorServico())
				.isTrue();
		assertThat(new FiltroDaBusca(null, null, null, null, CategoriaServico.UNHAS, null, null, null, null)
				.filtraPorServico()).isTrue();
		assertThat(comPrecos("10", null).filtraPorServico()).isTrue();
		assertThat(comPrecos(null, "90").filtraPorServico()).isTrue();
	}

	@Test
	void guardaAJanelaDeDisponibilidade() {
		JanelaDeDisponibilidade sexta = new JanelaDeDisponibilidade(LocalDate.of(2030, 3, 8), LocalTime.of(14, 0),
				null);

		FiltroDaBusca filtro = new FiltroDaBusca(null, null, null, null, null, null, null, null, sexta);

		assertThat(filtro.disponibilidade()).isEqualTo(sexta);
		assertThat(filtro.filtraPorServico()).isFalse();
	}

	@Test
	void aceitaFaixaDePrecoDeUmUnicoValor() {
		FiltroDaBusca filtro = comPrecos("80.00", "80");

		assertThat(filtro.precoMinimo()).isEqualByComparingTo("80");
		assertThat(filtro.precoMaximo()).isEqualByComparingTo("80");
	}

	@Test
	void recusaFaixaDePrecoInvertida() {
		assertThatThrownBy(() -> comPrecos("90", "50"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(FiltroDaBusca.FAIXA_DE_PRECO_INVERTIDA);
	}

	@ParameterizedTest(name = "preço {0}")
	@ValueSource(strings = { "-0.01", "-10" })
	void recusaPrecoNegativo(String preco) {
		assertThatThrownBy(() -> comPrecos(preco, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(FiltroDaBusca.PRECO_NEGATIVO);
		assertThatThrownBy(() -> comPrecos(null, preco))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(FiltroDaBusca.PRECO_NEGATIVO);
	}

	@ParameterizedTest(name = "nota mínima {0}")
	@ValueSource(strings = { "0", "3.5", "5" })
	void aceitaNotaMinimaDaEscala(String notaMinima) {
		assertThat(comNotaMinima(notaMinima).notaMinima()).isEqualByComparingTo(notaMinima);
	}

	@ParameterizedTest(name = "nota mínima {0}")
	@ValueSource(strings = { "-0.5", "5.01", "6" })
	void recusaNotaMinimaForaDaEscala(String notaMinima) {
		assertThatThrownBy(() -> comNotaMinima(notaMinima))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(FiltroDaBusca.NOTA_MINIMA_FORA_DA_ESCALA);
	}

	@Test
	void recusaTextoMaiorQueAColuna() {
		String nomeLongo = "a".repeat(151);

		assertThatThrownBy(() -> new FiltroDaBusca(nomeLongo, null, null, null, null, null, null, null, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessageContaining("nome");
	}

}
