package br.com.fiap.agendabeleza.api.notificacoes.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.notificacoes.application.NotificacaoGatewayEmMemoria;
import br.com.fiap.agendabeleza.api.notificacoes.application.NotificacaoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.notificacoes.application.usecase.EnviarNotificacoesPendentesUseCase;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;

/** O agendamento pelo Spring fica desligado nos testes; aqui a rodada é chamada direto. */
class EnvioDeNotificacoesAgendadoTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");

	private final NotificacaoRepositoryEmMemoria notificacoes = new NotificacaoRepositoryEmMemoria();
	private final NotificacaoGatewayEmMemoria servidorDeEmail = new NotificacaoGatewayEmMemoria();
	private final EnvioDeNotificacoesAgendado job = new EnvioDeNotificacoesAgendado(
			new EnviarNotificacoesPendentesUseCase(notificacoes, servidorDeEmail, Clock.fixed(AGORA, ZoneOffset.UTC)));

	@Test
	void cadaRodadaEnviaAsPendentesVencidas() {
		Mensagem mensagem = new Mensagem("cliente@email.com", "Agendamento reservado", "Reservado.");
		notificacoes.salvarTodas(List.of(Notificacao.pendente(UUID.randomUUID(), TipoNotificacao.CONFIRMACAO,
				PapelDestinatario.CLIENTE, mensagem, AGORA)));

		job.enviarPendentes();
		job.enviarPendentes();

		assertThat(servidorDeEmail.enviadas()).containsExactly(mensagem);
	}

	@Test
	void rodadaSemPendentesNaoEnviaNada() {
		job.enviarPendentes();

		assertThat(servidorDeEmail.enviadas()).isEmpty();
	}

}
