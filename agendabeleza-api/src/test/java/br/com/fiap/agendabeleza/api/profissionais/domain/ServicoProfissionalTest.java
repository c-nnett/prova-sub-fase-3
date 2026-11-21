package br.com.fiap.agendabeleza.api.profissionais.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class ServicoProfissionalTest {

	private static final UUID SERVICO_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

	@Test
	void guardaTarifaComDuasCasas() {
		ServicoProfissional servico = new ServicoProfissional(SERVICO_ID, new BigDecimal("95.5"));

		assertThat(servico.servicoId()).isEqualTo(SERVICO_ID);
		assertThat(servico.tarifa()).isEqualTo(new BigDecimal("95.50"));
	}

	@Test
	void aceitaTarifaZero() {
		assertThat(new ServicoProfissional(SERVICO_ID, BigDecimal.ZERO).tarifa()).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	void recusaTarifaNegativaOuComMaisDeDuasCasas() {
		assertThatThrownBy(() -> new ServicoProfissional(SERVICO_ID, new BigDecimal("-1")))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("tarifa não pode ser negativa");
		assertThatThrownBy(() -> new ServicoProfissional(SERVICO_ID, new BigDecimal("10.555")))
				.hasMessage("tarifa deve ter no máximo duas casas decimais");
	}

	@Test
	void recusaCamposAusentes() {
		assertThatThrownBy(() -> new ServicoProfissional(null, BigDecimal.TEN))
				.hasMessage("servicoId é obrigatório");
		assertThatThrownBy(() -> new ServicoProfissional(SERVICO_ID, null))
				.hasMessage("tarifa é obrigatório");
	}

}
