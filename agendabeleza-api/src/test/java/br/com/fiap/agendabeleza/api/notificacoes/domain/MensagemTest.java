package br.com.fiap.agendabeleza.api.notificacoes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.com.fiap.agendabeleza.api.compartilhado.domain.RegraNegocioException;

class MensagemTest {

	private static final String DESTINATARIO = "cliente@email.com";
	private static final String ASSUNTO = "Agendamento confirmado";
	private static final String CORPO = "Seu horário de corte está confirmado.";

	@Test
	void criaMensagemSemOsEspacosDasPontas() {
		Mensagem mensagem = new Mensagem(" " + DESTINATARIO + " ", " " + ASSUNTO + " ", CORPO + "\n");

		assertThat(mensagem.destinatario()).isEqualTo(DESTINATARIO);
		assertThat(mensagem.assunto()).isEqualTo(ASSUNTO);
		assertThat(mensagem.corpo()).isEqualTo(CORPO);
	}

	@Test
	void recusaDestinatarioInvalido() {
		assertThatThrownBy(() -> new Mensagem("cliente.email.com", ASSUNTO, CORPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("destinatario inválido");
		assertThatThrownBy(() -> new Mensagem(null, ASSUNTO, CORPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("destinatario é obrigatório");
	}

	@Test
	void recusaAssuntoAusenteOuLongoDemais() {
		assertThatThrownBy(() -> new Mensagem(DESTINATARIO, "  ", CORPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("assunto é obrigatório");
		assertThatThrownBy(() -> new Mensagem(DESTINATARIO, "a".repeat(201), CORPO))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("assunto deve ter no máximo 200 caracteres");
	}

	@Test
	void recusaCorpoAusenteOuLongoDemais() {
		assertThatThrownBy(() -> new Mensagem(DESTINATARIO, ASSUNTO, null))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("corpo é obrigatório");
		assertThatThrownBy(() -> new Mensagem(DESTINATARIO, ASSUNTO, "a".repeat(Mensagem.TAMANHO_MAXIMO_CORPO + 1)))
				.isInstanceOf(RegraNegocioException.class)
				.hasMessage("corpo deve ter no máximo " + Mensagem.TAMANHO_MAXIMO_CORPO + " caracteres");
	}

}
