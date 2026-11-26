package br.com.fiap.agendabeleza.api.notificacoes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TipoNotificacaoTest {

	private static final String DESTINATARIO = "cliente@email.com";
	/** 13h UTC são 10h em São Paulo. */
	private static final HorarioNotificado HORARIO = new HorarioNotificado("Salão Bela Vista", "Ana",
			Instant.parse("2030-03-05T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));

	@Test
	void confirmacaoInformaOHorarioNoFusoDoEstabelecimento() {
		Mensagem mensagem = TipoNotificacao.CONFIRMACAO.mensagemPara(DESTINATARIO, HORARIO);

		assertThat(mensagem.destinatario()).isEqualTo(DESTINATARIO);
		assertThat(mensagem.assunto()).isEqualTo("Agendamento reservado");
		assertThat(mensagem.corpo())
				.isEqualTo("O agendamento no Salão Bela Vista com Ana está reservado para 05/03/2030 às 10:00.");
	}

	@Test
	void cancelamentoInformaOHorarioLiberado() {
		Mensagem mensagem = TipoNotificacao.CANCELAMENTO.mensagemPara(DESTINATARIO, HORARIO);

		assertThat(mensagem.assunto()).isEqualTo("Agendamento cancelado");
		assertThat(mensagem.corpo())
				.isEqualTo("O agendamento no Salão Bela Vista com Ana em 05/03/2030 às 10:00 foi cancelado.");
	}

	@Test
	void lembreteInformaOHorarioQueSeAproxima() {
		Mensagem mensagem = TipoNotificacao.LEMBRETE.mensagemPara(DESTINATARIO, HORARIO);

		assertThat(mensagem.assunto()).isEqualTo("Lembrete de agendamento");
		assertThat(mensagem.corpo())
				.isEqualTo("Lembrete: o agendamento no Salão Bela Vista com Ana é em 05/03/2030 às 10:00.");
	}

	@Test
	void reagendamentoInformaONovoHorario() {
		Mensagem mensagem = TipoNotificacao.REAGENDAMENTO.mensagemPara(DESTINATARIO, HORARIO);

		assertThat(mensagem.assunto()).isEqualTo("Agendamento reagendado");
		assertThat(mensagem.corpo())
				.isEqualTo("O agendamento no Salão Bela Vista com Ana foi reagendado para 05/03/2030 às 10:00.");
	}

}
