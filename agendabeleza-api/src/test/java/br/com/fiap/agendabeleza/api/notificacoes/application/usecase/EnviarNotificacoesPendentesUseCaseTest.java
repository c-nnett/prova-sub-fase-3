package br.com.fiap.agendabeleza.api.notificacoes.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisoDoAgendamento;

import br.com.fiap.agendabeleza.api.notificacoes.application.NotificacaoGatewayEmMemoria;
import br.com.fiap.agendabeleza.api.notificacoes.application.NotificacaoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.StatusNotificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;

class EnviarNotificacoesPendentesUseCaseTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");

	private final NotificacaoRepositoryEmMemoria notificacoes = new NotificacaoRepositoryEmMemoria();
	private final NotificacaoGatewayEmMemoria servidorDeEmail = new NotificacaoGatewayEmMemoria();
	private final EnviarNotificacoesPendentesUseCase enviar = new EnviarNotificacoesPendentesUseCase(notificacoes,
			servidorDeEmail, Clock.fixed(AGORA, ZoneOffset.UTC));

	private Notificacao gravarPendente(String destinatario, Instant enviarEm) {
		Notificacao notificacao = Notificacao.pendente(UUID.randomUUID(), TipoNotificacao.CONFIRMACAO,
				PapelDestinatario.CLIENTE, new Mensagem(destinatario, "Agendamento reservado", "Reservado."),
				enviarEm);
		notificacoes.salvarTodas(List.of(notificacao));
		return notificacao;
	}

	@Test
	void enviaAsPendentesVencidasEMarcaComoEnviadas() {
		Notificacao vencida = gravarPendente("cliente@email.com", AGORA.minus(Duration.ofMinutes(1)));
		Notificacao noHorario = gravarPendente("ana@salao.com", AGORA);
		Notificacao futura = gravarPendente("bia@salao.com", AGORA.plus(Duration.ofHours(1)));

		int enviadas = enviar.executar();

		assertThat(enviadas).isEqualTo(2);
		assertThat(servidorDeEmail.enviadas())
				.containsExactly(vencida.mensagem(), noHorario.mensagem());
		assertThat(vencida.status()).isEqualTo(StatusNotificacao.ENVIADA);
		assertThat(vencida.enviadaEm()).isEqualTo(AGORA);
		assertThat(noHorario.status()).isEqualTo(StatusNotificacao.ENVIADA);
		assertThat(futura.status()).isEqualTo(StatusNotificacao.PENDENTE);
	}

	@Test
	void notificacaoCanceladaNaoEEnviada() {
		Notificacao cancelada = gravarPendente("cliente@email.com", AGORA);
		cancelada.cancelar();
		notificacoes.atualizar(cancelada);

		assertThat(enviar.executar()).isZero();
		assertThat(servidorDeEmail.enviadas()).isEmpty();
	}

	/** "Pronto quando" de 25/11: o agendamento cancelado não recebe lembrete, e o que não foi cancelado recebe. */
	@Test
	void lembreteSaiNaHoraSoParaQuemNaoCancelou() {
		Instant reservaFeitaEm = AGORA.minus(Duration.ofDays(3));
		AvisosPorEmail avisosNaReserva = new AvisosPorEmail(notificacoes, Clock.fixed(reservaFeitaEm, ZoneOffset.UTC));
		Instant inicio = AGORA.plus(Duration.ofHours(23));
		AvisoDoAgendamento mantido = aviso(inicio, "mantido@email.com");
		AvisoDoAgendamento cancelado = aviso(inicio, "cancelado@email.com");
		avisosNaReserva.reservado(mantido, "cliente.mantido@email.com");
		avisosNaReserva.reservado(cancelado, "cliente.cancelado@email.com");
		avisosNaReserva.cancelado(cancelado);

		enviar.executar();

		assertThat(servidorDeEmail.enviadas())
				.filteredOn(mensagem -> mensagem.assunto().equals("Lembrete de agendamento"))
				.extracting(Mensagem::destinatario)
				.containsExactlyInAnyOrder("cliente.mantido@email.com", "mantido@email.com");
	}

	@Test
	void lembreteEnviadoNaoSaiDeNovo() {
		gravarPendente("cliente@email.com", AGORA);

		enviar.executar();
		int enviadasNaSegundaRodada = enviar.executar();

		assertThat(enviadasNaSegundaRodada).isZero();
		assertThat(servidorDeEmail.enviadas()).hasSize(1);
	}

	private static AvisoDoAgendamento aviso(Instant inicio, String emailProfissional) {
		return new AvisoDoAgendamento(UUID.randomUUID(), inicio, ZoneId.of("America/Sao_Paulo"), "Salão Bela Vista",
				"Ana", emailProfissional);
	}

	@Test
	void falhaDeUmaNotificacaoNaoInterrompeAsDemais() {
		Notificacao recusada = gravarPendente("cliente@email.com", AGORA.minus(Duration.ofMinutes(2)));
		Notificacao aceita = gravarPendente("ana@salao.com", AGORA.minus(Duration.ofMinutes(1)));
		servidorDeEmail.recusar("cliente@email.com");

		int enviadas = enviar.executar();

		assertThat(enviadas).isEqualTo(1);
		assertThat(recusada.status()).isEqualTo(StatusNotificacao.PENDENTE);
		assertThat(recusada.tentativas()).isEqualTo(1);
		assertThat(recusada.ultimoErro()).isEqualTo(NotificacaoGatewayEmMemoria.ERRO_DO_SERVIDOR);
		assertThat(aceita.status()).isEqualTo(StatusNotificacao.ENVIADA);
	}

	@Test
	void desisteDaNotificacaoDepoisDoMaximoDeTentativas() {
		Notificacao recusada = gravarPendente("cliente@email.com", AGORA);
		servidorDeEmail.recusar("cliente@email.com");

		for (int rodada = 0; rodada < Notificacao.MAXIMO_DE_TENTATIVAS; rodada++) {
			enviar.executar();
		}
		int enviadasDepois = enviar.executar();

		assertThat(recusada.status()).isEqualTo(StatusNotificacao.FALHA);
		assertThat(recusada.tentativas()).isEqualTo(Notificacao.MAXIMO_DE_TENTATIVAS);
		assertThat(enviadasDepois).isZero();
		assertThat(servidorDeEmail.enviadas()).isEmpty();
	}

}
