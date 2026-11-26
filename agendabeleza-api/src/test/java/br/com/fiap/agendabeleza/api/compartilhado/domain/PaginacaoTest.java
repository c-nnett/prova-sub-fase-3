package br.com.fiap.agendabeleza.api.compartilhado.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PaginacaoTest {

	@Test
	void semValoresUsaAPrimeiraPaginaComOTamanhoPadrao() {
		Paginacao paginacao = Paginacao.de(null, null);

		assertThat(paginacao.numero()).isZero();
		assertThat(paginacao.tamanho()).isEqualTo(Paginacao.TAMANHO_PADRAO);
	}

	@Test
	void calculaODeslocamentoDaPagina() {
		Paginacao paginacao = Paginacao.de(3, 10);

		assertThat(paginacao.deslocamento()).isEqualTo(30);
	}

	@Test
	void aceitaOTamanhoMaximo() {
		assertThat(Paginacao.de(0, Paginacao.TAMANHO_MAXIMO).tamanho()).isEqualTo(Paginacao.TAMANHO_MAXIMO);
	}

	@Test
	void recusaPaginaNegativa() {
		assertThatThrownBy(() -> Paginacao.de(-1, 10))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(Paginacao.PAGINA_NEGATIVA);
	}

	@ParameterizedTest(name = "tamanho {0}")
	@ValueSource(ints = { 0, -1, Paginacao.TAMANHO_MAXIMO + 1 })
	void recusaTamanhoForaDoLimite(int tamanho) {
		assertThatThrownBy(() -> Paginacao.de(0, tamanho))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage(Paginacao.TAMANHO_FORA_DO_LIMITE);
	}

}
