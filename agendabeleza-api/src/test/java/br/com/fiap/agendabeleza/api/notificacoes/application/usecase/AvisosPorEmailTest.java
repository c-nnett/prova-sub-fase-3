package br.com.fiap.agendabeleza.api.notificacoes.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisoDoAgendamento;
import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;
import br.com.fiap.agendabeleza.api.notificacoes.application.NotificacaoRepositoryEmMemoria;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.PapelDestinatario;
import br.com.fiap.agendabeleza.api.notificacoes.domain.StatusNotificacao;
import br.com.fiap.agendabeleza.api.notificacoes.domain.TipoNotificacao;

class AvisosPorEmailTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");
	private static final String EMAIL_CLIENTE = "cliente@email.com";
	private static final String EMAIL_PROFISSIONAL = "ana@salao.com";

	private final NotificacaoRepositoryEmMemoria notificacoes = new NotificacaoRepositoryEmMemoria();
	private final AvisosPorEmail avisos = new AvisosPorEmail(notificacoes, Clock.fixed(AGORA, ZoneOffset.UTC));
	/** Início em 05/03, 10h em São Paulo: 25h depois de agora, então o lembrete fica para 04/03 às 13h UTC. */
	private static final Instant INICIO = Instant.parse("2030-03-05T13:00:00Z");
	private static final Instant VINTE_E_QUATRO_HORAS_ANTES = Instant.parse("2030-03-04T13:00:00Z");

	private final UUID agendamentoId = UUID.randomUUID();
	private final AvisoDoAgendamento aviso = avisoComInicio(INICIO);

	private AvisoDoAgendamento avisoComInicio(Instant inicio) {
		return new AvisoDoAgendamento(agendamentoId, inicio, ZoneId.of("America/Sao_Paulo"), "Salão Bela Vista",
				"Ana", EMAIL_PROFISSIONAL);
	}

	private List<Notificacao> doTipo(TipoNotificacao tipo) {
		return notificacoes.todas().stream().filter(notificacao -> notificacao.tipo() == tipo).toList();
	}

	@Test
	void reservaGravaUmaConfirmacaoParaOClienteEOutraParaOProfissional() {
		avisos.reservado(aviso, EMAIL_CLIENTE);

		assertThat(doTipo(TipoNotificacao.CONFIRMACAO))
				.extracting(Notificacao::agendamentoId, Notificacao::tipo, Notificacao::papel,
						notificacao -> notificacao.mensagem().destinatario(), Notificacao::status,
						Notificacao::enviarEm)
				.containsExactlyInAnyOrder(
						tuple(aviso.agendamentoId(), TipoNotificacao.CONFIRMACAO, PapelDestinatario.CLIENTE,
								EMAIL_CLIENTE, StatusNotificacao.PENDENTE, AGORA),
						tuple(aviso.agendamentoId(), TipoNotificacao.CONFIRMACAO, PapelDestinatario.PROFISSIONAL,
								EMAIL_PROFISSIONAL, StatusNotificacao.PENDENTE, AGORA));
		assertThat(doTipo(TipoNotificacao.CONFIRMACAO))
				.extracting(notificacao -> notificacao.mensagem().corpo())
				.containsOnly("O agendamento no Salão Bela Vista com Ana está reservado para 05/03/2030 às 10:00.");
	}

	@Test
	void reservaAgendaUmLembretePorDestinatario24HorasAntesDoInicio() {
		avisos.reservado(aviso, EMAIL_CLIENTE);

		assertThat(doTipo(TipoNotificacao.LEMBRETE))
				.extracting(Notificacao::papel, notificacao -> notificacao.mensagem().destinatario(),
						Notificacao::status, Notificacao::enviarEm)
				.containsExactlyInAnyOrder(
						tuple(PapelDestinatario.CLIENTE, EMAIL_CLIENTE, StatusNotificacao.PENDENTE,
								VINTE_E_QUATRO_HORAS_ANTES),
						tuple(PapelDestinatario.PROFISSIONAL, EMAIL_PROFISSIONAL, StatusNotificacao.PENDENTE,
								VINTE_E_QUATRO_HORAS_ANTES));
	}

	@Test
	void reservaComMenosDe24HorasDeAntecedenciaNaoGanhaLembrete() {
		avisos.reservado(avisoComInicio(AGORA.plus(Duration.ofHours(2))), EMAIL_CLIENTE);

		assertThat(doTipo(TipoNotificacao.LEMBRETE)).isEmpty();
		assertThat(doTipo(TipoNotificacao.CONFIRMACAO)).hasSize(2);
	}

	@Test
	void cancelamentoCancelaOsLembretesPendentes() {
		avisos.reservado(aviso, EMAIL_CLIENTE);

		avisos.cancelado(aviso);

		assertThat(doTipo(TipoNotificacao.LEMBRETE))
				.hasSize(2)
				.extracting(Notificacao::status)
				.containsOnly(StatusNotificacao.CANCELADA);
		assertThat(doTipo(TipoNotificacao.CONFIRMACAO))
				.extracting(Notificacao::status)
				.containsOnly(StatusNotificacao.PENDENTE);
	}

	@Test
	void cancelamentoNaoMexeNoLembreteJaEnviado() {
		avisos.reservado(aviso, EMAIL_CLIENTE);
		Notificacao enviado = doTipo(TipoNotificacao.LEMBRETE).getFirst();
		enviado.marcarEnviada(VINTE_E_QUATRO_HORAS_ANTES);
		notificacoes.atualizar(enviado);

		avisos.cancelado(aviso);

		assertThat(enviado.status()).isEqualTo(StatusNotificacao.ENVIADA);
		assertThat(doTipo(TipoNotificacao.LEMBRETE))
				.extracting(Notificacao::status)
				.containsExactlyInAnyOrder(StatusNotificacao.ENVIADA, StatusNotificacao.CANCELADA);
	}

	@Test
	void reagendamentoTrocaOsLembretesPelosDoNovoHorario() {
		avisos.reservado(aviso, EMAIL_CLIENTE);
		Instant novoInicio = INICIO.plus(Duration.ofDays(1));

		avisos.reagendado(avisoComInicio(novoInicio));

		assertThat(doTipo(TipoNotificacao.LEMBRETE))
				.extracting(Notificacao::status, Notificacao::enviarEm)
				.containsExactlyInAnyOrder(
						tuple(StatusNotificacao.CANCELADA, VINTE_E_QUATRO_HORAS_ANTES),
						tuple(StatusNotificacao.CANCELADA, VINTE_E_QUATRO_HORAS_ANTES),
						tuple(StatusNotificacao.PENDENTE, INICIO),
						tuple(StatusNotificacao.PENDENTE, INICIO));
		assertThat(doTipo(TipoNotificacao.LEMBRETE))
				.filteredOn(notificacao -> notificacao.status() == StatusNotificacao.PENDENTE)
				.extracting(notificacao -> notificacao.mensagem().destinatario())
				.containsExactlyInAnyOrder(EMAIL_CLIENTE, EMAIL_PROFISSIONAL);
	}

	@Test
	void cancelamentoReaproveitaOEmailDoClienteDaReserva() {
		avisos.reservado(aviso, EMAIL_CLIENTE);

		avisos.cancelado(aviso);

		assertThat(notificacoes.todas())
				.filteredOn(notificacao -> notificacao.tipo() == TipoNotificacao.CANCELAMENTO)
				.extracting(Notificacao::papel, notificacao -> notificacao.mensagem().destinatario())
				.containsExactlyInAnyOrder(
						tuple(PapelDestinatario.CLIENTE, EMAIL_CLIENTE),
						tuple(PapelDestinatario.PROFISSIONAL, EMAIL_PROFISSIONAL));
	}

	@Test
	void reagendamentoAvisaOsDoisComONovoHorario() {
		avisos.reservado(aviso, EMAIL_CLIENTE);

		avisos.reagendado(aviso);

		assertThat(notificacoes.todas())
				.filteredOn(notificacao -> notificacao.tipo() == TipoNotificacao.REAGENDAMENTO)
				.hasSize(2)
				.extracting(notificacao -> notificacao.mensagem().corpo())
				.containsOnly("O agendamento no Salão Bela Vista com Ana foi reagendado para 05/03/2030 às 10:00.");
	}

	@Test
	void semEmailDoClienteRegistradoAvisaSoOProfissional() {
		avisos.cancelado(aviso);

		assertThat(notificacoes.todas())
				.singleElement()
				.satisfies(notificacao -> {
					assertThat(notificacao.papel()).isEqualTo(PapelDestinatario.PROFISSIONAL);
					assertThat(notificacao.mensagem().destinatario()).isEqualTo(EMAIL_PROFISSIONAL);
				});
	}

	@Test
	void recusaEmailDoClienteInvalidoSemGravarNada() {
		assertThatThrownBy(() -> avisos.reservado(aviso, "cliente.email.com"))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("destinatario inválido");
		assertThat(notificacoes.todas()).isEmpty();
	}

}
