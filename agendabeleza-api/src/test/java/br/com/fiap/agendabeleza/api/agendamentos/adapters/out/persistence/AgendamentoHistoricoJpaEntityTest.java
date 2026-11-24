package br.com.fiap.agendabeleza.api.agendamentos.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AgendamentoHistoricoJpaEntityTest {

	/** A trilha é somente inserção: o Spring Data deve usar {@code persist}, sem SELECT pelo id antes. */
	@Test
	void linhaDoHistoricoComIdAtribuidoESempreNova() {
		AgendamentoHistoricoJpaEntity linha = new AgendamentoHistoricoJpaEntity();
		linha.setId(UUID.randomUUID());

		assertThat(linha.isNew()).isTrue();
		assertThat(linha.getId()).isNotNull();
	}

}
