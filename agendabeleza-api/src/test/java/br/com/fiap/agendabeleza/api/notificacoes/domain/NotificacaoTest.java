package br.com.fiap.agendabeleza.api.notificacoes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class NotificacaoTest {

	private static final Instant AGORA = Instant.parse("2030-03-04T12:00:00Z");
	private static final Mensagem MENSAGEM = new Mensagem("cliente@email.com", "Agendamento reservado",
			"O agendamento está reservado.");

	private static Notificacao pendente() {
		return Notificacao.pendente(UUID.randomUUID(), TipoNotificacao.CONFIRMACAO, PapelDestinatario.CLIENTE,
				MENSAGEM, AGORA);
	}

	@Test
	void nascePendenteSemTentativas() {
		UUID agendamentoId = UUID.randomUUID();

		Notificacao notificacao = Notificacao.pendente(agendamentoId, TipoNotificacao.CONFIRMACAO,
				PapelDestinatario.CLIENTE, MENSAGEM, AGORA);

		assertThat(notificacao.id()).isNotNull();
		assertThat(notificacao.agendamentoId()).isEqualTo(agendamentoId);
		assertThat(notificacao.tipo()).isEqualTo(TipoNotificacao.CONFIRMACAO);
		assertThat(notificacao.papel()).isEqualTo(PapelDestinatario.CLIENTE);
		assertThat(notificacao.mensagem()).isEqualTo(MENSAGEM);
		assertThat(notificacao.enviarEm()).isEqualTo(AGORA);
		assertThat(notificacao.status()).isEqualTo(StatusNotificacao.PENDENTE);
		assertThat(notificacao.tentativas()).isZero();
		assertThat(notificacao.enviadaEm()).isNull();
		assertThat(notificacao.ultimoErro()).isNull();
	}

	@Test
	void recusaCamposObrigatoriosAusentes() {
		assertThatThrownBy(() -> Notificacao.pendente(null, TipoNotificacao.CONFIRMACAO, PapelDestinatario.CLIENTE,
				MENSAGEM, AGORA)).isInstanceOf(RegraNegocioException.class).hasMessage("agendamentoId é obrigatório");
		assertThatThrownBy(() -> Notificacao.pendente(UUID.randomUUID(), TipoNotificacao.CONFIRMACAO,
				PapelDestinatario.CLIENTE, null, AGORA)).hasMessage("mensagem é obrigatório");
		assertThatThrownBy(() -> Notificacao.pendente(UUID.randomUUID(), TipoNotificacao.CONFIRMACAO,
				PapelDestinatario.CLIENTE, MENSAGEM, null)).hasMessage("enviarEm é obrigatório");
	}

	@Test
	void marcaComoEnviada() {
		Notificacao notificacao = pendente();

		notificacao.marcarEnviada(AGORA);

		assertThat(notificacao.status()).isEqualTo(StatusNotificacao.ENVIADA);
		assertThat(notificacao.enviadaEm()).isEqualTo(AGORA);
	}

	@Test
	void naoEnviaDuasVezes() {
		Notificacao notificacao = pendente();
		notificacao.marcarEnviada(AGORA);

		assertThatThrownBy(() -> notificacao.marcarEnviada(AGORA))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("Somente notificação pendente pode ser enviada");
	}

	@Test
	void falhaContaTentativaEContinuaPendente() {
		Notificacao notificacao = pendente();

		notificacao.registrarFalha("Conexão recusada");

		assertThat(notificacao.status()).isEqualTo(StatusNotificacao.PENDENTE);
		assertThat(notificacao.tentativas()).isEqualTo(1);
		assertThat(notificacao.ultimoErro()).isEqualTo("Conexão recusada");
	}

	@Test
	void desisteDepoisDoMaximoDeTentativas() {
		Notificacao notificacao = pendente();

		for (int tentativa = 1; tentativa < Notificacao.MAXIMO_DE_TENTATIVAS; tentativa++) {
			notificacao.registrarFalha("Conexão recusada");
		}
		assertThat(notificacao.status()).isEqualTo(StatusNotificacao.PENDENTE);

		notificacao.registrarFalha("Conexão recusada");

		assertThat(notificacao.status()).isEqualTo(StatusNotificacao.FALHA);
		assertThat(notificacao.tentativas()).isEqualTo(Notificacao.MAXIMO_DE_TENTATIVAS);
	}

	@Test
	void reconstituiOsDadosGravados() {
		UUID id = UUID.randomUUID();
		UUID agendamentoId = UUID.randomUUID();

		Notificacao notificacao = Notificacao.reconstituir(id, agendamentoId, TipoNotificacao.CANCELAMENTO,
				PapelDestinatario.PROFISSIONAL, MENSAGEM, AGORA, StatusNotificacao.ENVIADA, 2, AGORA, "Timeout");

		assertThat(notificacao.id()).isEqualTo(id);
		assertThat(notificacao.agendamentoId()).isEqualTo(agendamentoId);
		assertThat(notificacao.tipo()).isEqualTo(TipoNotificacao.CANCELAMENTO);
		assertThat(notificacao.papel()).isEqualTo(PapelDestinatario.PROFISSIONAL);
		assertThat(notificacao.status()).isEqualTo(StatusNotificacao.ENVIADA);
		assertThat(notificacao.tentativas()).isEqualTo(2);
		assertThat(notificacao.enviadaEm()).isEqualTo(AGORA);
		assertThat(notificacao.ultimoErro()).isEqualTo("Timeout");
	}

}
