package br.com.fiap.agendabeleza.api.notificacoes.adapters.out.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;

import br.com.fiap.agendabeleza.api.notificacoes.domain.FalhaNoEnvioException;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.MimeMessage;

/**
 * Envia de verdade por SMTP para um servidor em memória (GreenMail), sem subir o Spring.
 */
class NotificacaoSmtpAdapterTest {

	private static final String REMETENTE = "nao-responda@agendabeleza.local";
	private static final Mensagem MENSAGEM = new Mensagem("cliente@email.com", "Agendamento confirmado",
			"Seu horário de corte às 10h está confirmado.");

	@RegisterExtension
	static final GreenMailExtension servidorDeEmail = new GreenMailExtension(ServerSetupTest.SMTP);

	@Test
	void enviaMensagemComRemetenteDestinatarioAssuntoECorpo() throws Exception {
		NotificacaoSmtpAdapter adaptador = adaptadorNaPorta(servidorDeEmail.getSmtp().getPort());

		adaptador.enviar(MENSAGEM);

		MimeMessage[] recebidas = servidorDeEmail.getReceivedMessages();
		assertThat(recebidas).hasSize(1);
		MimeMessage recebida = recebidas[0];
		assertThat(recebida.getFrom()[0]).hasToString(REMETENTE);
		assertThat(recebida.getRecipients(RecipientType.TO)[0]).hasToString(MENSAGEM.destinatario());
		assertThat(recebida.getSubject()).isEqualTo(MENSAGEM.assunto());
		assertThat(recebida.getContent().toString().strip()).isEqualTo(MENSAGEM.corpo());
	}

	@Test
	void traduzFalhaDoServidorDeEmailParaFalhaNoEnvio() throws IOException {
		NotificacaoSmtpAdapter adaptador = adaptadorNaPorta(portaSemServidor());

		assertThatThrownBy(() -> adaptador.enviar(MENSAGEM))
				.isInstanceOf(FalhaNoEnvioException.class)
				.hasMessageContaining(MENSAGEM.destinatario());
	}

	private static NotificacaoSmtpAdapter adaptadorNaPorta(int porta) {
		JavaMailSenderImpl envioDeEmail = new JavaMailSenderImpl();
		envioDeEmail.setHost("localhost");
		envioDeEmail.setPort(porta);
		envioDeEmail.setDefaultEncoding(StandardCharsets.UTF_8.name());
		return new NotificacaoSmtpAdapter(envioDeEmail, REMETENTE);
	}

	/** Reserva uma porta livre e a libera, para que a conexão SMTP seja recusada. */
	private static int portaSemServidor() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

}
