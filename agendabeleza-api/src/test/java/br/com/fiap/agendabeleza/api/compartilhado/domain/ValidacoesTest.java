package br.com.fiap.agendabeleza.api.compartilhado.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ValidacoesTest {

	private static final String CAMPO = "destinatario";

	@Test
	void aceitaEmailValidoSemOsEspacosDasPontas() {
		assertThat(Validacoes.email("  ana@salao.com ", CAMPO)).isEqualTo("ana@salao.com");
	}

	@Test
	void recusaEmailAusente() {
		assertThatThrownBy(() -> Validacoes.email(null, CAMPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("destinatario é obrigatório");
		assertThatThrownBy(() -> Validacoes.email("   ", CAMPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("destinatario é obrigatório");
	}

	@Test
	void recusaEmailSemFormatoValido() {
		assertThatThrownBy(() -> Validacoes.email("ana.salao.com", CAMPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("destinatario inválido");
		assertThatThrownBy(() -> Validacoes.email("ana@salao", CAMPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("destinatario inválido");
	}

	@Test
	void recusaEmailAcimaDoTamanhoMaximo() {
		String emailLongo = "a".repeat(171) + "@salao.com";

		assertThatThrownBy(() -> Validacoes.email(emailLongo, CAMPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("destinatario deve ter no máximo 180 caracteres");
	}

}
