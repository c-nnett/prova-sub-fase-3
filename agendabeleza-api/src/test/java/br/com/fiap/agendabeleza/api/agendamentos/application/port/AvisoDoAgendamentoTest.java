package br.com.fiap.agendabeleza.api.agendamentos.application.port;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.agendamentos.domain.Agendamento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.Estabelecimento;
import br.com.fiap.agendabeleza.api.estabelecimentos.domain.EstabelecimentoFixture;
import br.com.fiap.agendabeleza.api.profissionais.domain.Profissional;
import br.com.fiap.agendabeleza.api.profissionais.domain.ProfissionalFixture;

class AvisoDoAgendamentoTest {

	@Test
	void levaDoAgendamentoSoOQueOAvisoMostra() {
		Estabelecimento estabelecimento = EstabelecimentoFixture.estabelecimento();
		Profissional profissional = ProfissionalFixture.profissional(estabelecimento.id());
		Instant inicio = Instant.parse("2030-03-05T13:00:00Z");
		Agendamento agendamento = Agendamento.reservar(UUID.randomUUID(), estabelecimento.id(), profissional.id(),
				UUID.randomUUID(), inicio, 60, new BigDecimal("95.00"), null, Instant.parse("2030-03-04T12:00:00Z"));

		AvisoDoAgendamento aviso = AvisoDoAgendamento.de(agendamento, profissional, estabelecimento);

		assertThat(aviso.agendamentoId()).isEqualTo(agendamento.id());
		assertThat(aviso.inicio()).isEqualTo(inicio);
		assertThat(aviso.fuso()).isEqualTo(estabelecimento.timezone());
		assertThat(aviso.nomeEstabelecimento()).isEqualTo("Salão Bela Vista");
		assertThat(aviso.nomeProfissional()).isEqualTo("Ana Souza");
		assertThat(aviso.emailProfissional()).isEqualTo("ana@salao.com");
	}

}
