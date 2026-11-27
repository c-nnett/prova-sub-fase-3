package br.com.fiap.agendabeleza.api.calendario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RecursoNaoEncontradoException;

class TokenDoCalendarioTest {

	private static final String TOKEN_VALIDO = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJ0123_-9";

	@Test
	void geraTokenDe43CaracteresEmBase64ParaUrl() {
		TokenDoCalendario token = TokenDoCalendario.gerar();

		assertThat(token.valor()).hasSize(TokenDoCalendario.TAMANHO).matches("[A-Za-z0-9_-]+");
	}

	@Test
	void geraTokensDiferentesACadaChamada() {
		assertThat(TokenDoCalendario.gerar()).isNotEqualTo(TokenDoCalendario.gerar());
	}

	@Test
	void aceitaTokenNoFormatoGerado() {
		TokenDoCalendario token = TokenDoCalendario.de(TOKEN_VALIDO);

		assertThat(token.valor()).isEqualTo(TOKEN_VALIDO);
		assertThat(token).isEqualTo(TokenDoCalendario.de(TOKEN_VALIDO));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "curto", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJ0123+/9",
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJ0123_-9x" })
	void tokenMalformadoEhTratadoComoCalendarioInexistente(String valor) {
		assertThatThrownBy(() -> TokenDoCalendario.de(valor))
				.isInstanceOf(RecursoNaoEncontradoException.class)
				.hasMessage(TokenDoCalendario.CALENDARIO_NAO_ENCONTRADO);
	}

	@Test
	void hashEhSha256EmHexadecimalEDeterministico() {
		TokenDoCalendario token = TokenDoCalendario.de(TOKEN_VALIDO);

		assertThat(token.hash()).matches("[0-9a-f]{64}").isEqualTo(TokenDoCalendario.de(TOKEN_VALIDO).hash());
		assertThat(token.hash()).isNotEqualTo(TokenDoCalendario.gerar().hash());
	}

	@Test
	void textoDoObjetoNaoExpoeOToken() {
		assertThat(TokenDoCalendario.de(TOKEN_VALIDO).toString()).doesNotContain(TOKEN_VALIDO);
	}

}
