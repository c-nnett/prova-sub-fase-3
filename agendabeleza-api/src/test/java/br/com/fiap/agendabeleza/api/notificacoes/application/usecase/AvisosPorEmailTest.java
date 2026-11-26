package br.com.fiap.agendabeleza.api.notificacoes.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
	private final AvisoDoAgendamento aviso = new AvisoDoAgendamento(UUID.randomUUID(),
			Instant.parse("2030-03-05T13:00:00Z"), ZoneId.of("America/Sao_Paulo"), "Salão Bela Vista", "Ana",
			EMAIL_PROFISSIONAL);

	@Test
	void reservaGravaUmaConfirmacaoParaOClienteEOutraParaOProfissional() {
		avisos.reservado(aviso, EMAIL_CLIENTE);

		assertThat(notificacoes.todas())
				.extracting(Notificacao::agendamentoId, Notificacao::tipo, Notificacao::papel,
						notificacao -> notificacao.mensagem().destinatario(), Notificacao::status,
						Notificacao::enviarEm)
				.containsExactlyInAnyOrder(
						tuple(aviso.agendamentoId(), TipoNotificacao.CONFIRMACAO, PapelDestinatario.CLIENTE,
								EMAIL_CLIENTE, StatusNotificacao.PENDENTE, AGORA),
						tuple(aviso.agendamentoId(), TipoNotificacao.CONFIRMACAO, PapelDestinatario.PROFISSIONAL,
								EMAIL_PROFISSIONAL, StatusNotificacao.PENDENTE, AGORA));
		assertThat(notificacoes.todas())
				.extracting(notificacao -> notificacao.mensagem().corpo())
				.containsOnly("O agendamento no Salão Bela Vista com Ana está reservado para 05/03/2030 às 10:00.");
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
