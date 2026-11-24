package br.com.fiap.agendabeleza.api.profissionais.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class EspecialidadeTest {

	@Test
	void cadastraComNomeSemEspacosNasPontas() {
		Especialidade especialidade = Especialidade.cadastrar("  Colorimetria ");

		assertThat(especialidade.id()).isNotNull();
		assertThat(especialidade.nome()).isEqualTo("Colorimetria");
	}

	@Test
	void recusaNomeAusenteOuAcimaDe80Caracteres() {
		assertThatThrownBy(() -> Especialidade.cadastrar(" "))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("nome é obrigatório");
		assertThatThrownBy(() -> Especialidade.cadastrar("a".repeat(81)))
				.hasMessage("nome deve ter no máximo 80 caracteres");
	}

	@Test
	void reconstituiComOsDadosGravados() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

		Especialidade especialidade = Especialidade.reconstituir(id, "Manicure");

		assertThat(especialidade.id()).isEqualTo(id);
		assertThat(especialidade.nome()).isEqualTo("Manicure");
	}

	@Test
	void reconstituicaoExigeId() {
		assertThatThrownBy(() -> Especialidade.reconstituir(null, "Manicure"))
				.hasMessage("id é obrigatório");
	}

}
