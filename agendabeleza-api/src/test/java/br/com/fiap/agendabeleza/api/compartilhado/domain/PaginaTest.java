package br.com.fiap.agendabeleza.api.compartilhado.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PaginaTest {

	@ParameterizedTest(name = "{0} itens de {1} em {1} → {2} páginas")
	@CsvSource({
			"0, 10, 0",
			"1, 10, 1",
			"10, 10, 1",
			"11, 10, 2",
			"45, 20, 3" })
	void calculaOTotalDePaginas(long totalDeItens, int tamanho, int totalDePaginasEsperado) {
		Pagina<String> pagina = Pagina.de(List.of(), Paginacao.de(0, tamanho), totalDeItens);

		assertThat(pagina.totalDePaginas()).isEqualTo(totalDePaginasEsperado);
	}

	@Test
	void guardaONumeroEOTamanhoDaPaginacao() {
		Pagina<String> pagina = Pagina.de(List.of("Salão"), Paginacao.de(2, 5), 11);

		assertThat(pagina.numero()).isEqualTo(2);
		assertThat(pagina.tamanho()).isEqualTo(5);
		assertThat(pagina.totalDeItens()).isEqualTo(11);
		assertThat(pagina.itens()).containsExactly("Salão");
	}

	@Test
	void converteOsItensMantendoOsTotais() {
		Pagina<String> pagina = Pagina.de(List.of("Ana", "Bia"), Paginacao.de(1, 2), 4);

		Pagina<Integer> convertida = pagina.converter(String::length);

		assertThat(convertida.itens()).containsExactly(3, 3);
		assertThat(convertida.numero()).isEqualTo(1);
		assertThat(convertida.tamanho()).isEqualTo(2);
		assertThat(convertida.totalDeItens()).isEqualTo(4);
	}

	@Test
	void naoAcompanhaAlteracoesDaListaOriginal() {
		List<String> itens = new ArrayList<>(List.of("Ana"));
		Pagina<String> pagina = Pagina.de(itens, Paginacao.de(0, 10), 1);

		itens.add("Bia");

		assertThat(pagina.itens()).containsExactly("Ana");
	}

	@Test
	void recusaTotalNegativo() {
		Paginacao paginacao = Paginacao.de(0, 10);
		List<String> semItens = List.of();

		assertThatThrownBy(() -> Pagina.de(semItens, paginacao, -1))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
