package br.com.fiap.agendabeleza.api.estabelecimentos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class EnderecoTest {

	@Test
	void normalizaUfCepEComplemento() {
		Endereco endereco = new Endereco(" Rua Augusta ", "100", "  ", "Consolação", "São Paulo", "sp", "01305-000");

		assertThat(endereco.logradouro()).isEqualTo("Rua Augusta");
		assertThat(endereco.uf()).isEqualTo("SP");
		assertThat(endereco.cep()).isEqualTo("01305000");
		assertThat(endereco.complemento()).isNull();
	}

	@Test
	void mantemComplementoInformado() {
		Endereco endereco = new Endereco("Rua Augusta", "100", "Sala 2", "Consolação", "São Paulo", "SP", "01305000");

		assertThat(endereco.complemento()).isEqualTo("Sala 2");
	}

	@Test
	void recusaCampoObrigatorioAusente() {
		assertThatThrownBy(() -> new Endereco(null, "100", null, "Consolação", "São Paulo", "SP", "01305000"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("logradouro é obrigatório");
	}

	@Test
	void recusaCampoAcimaDoTamanhoMaximo() {
		assertThatThrownBy(() -> new Endereco("Rua", "12345678901", null, "Centro", "São Paulo", "SP", "01305000"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("numero deve ter no máximo 10 caracteres");
	}

	@Test
	void recusaUfInvalida() {
		assertThatThrownBy(() -> new Endereco("Rua", "1", null, "Centro", "São Paulo", "S1", "01305000"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("uf deve ter duas letras");
	}

	@Test
	void recusaCepInvalido() {
		assertThatThrownBy(() -> new Endereco("Rua", "1", null, "Centro", "São Paulo", "SP", "1234"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("cep deve ter oito dígitos");
	}

}
