package br.com.fiap.agendabeleza.api.notificacoes.adapters.out.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoGateway;
import br.com.fiap.agendabeleza.api.notificacoes.domain.FalhaNoEnvioException;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;

/**
 * Implementa {@link NotificacaoGateway} por SMTP com o {@link JavaMailSender} do Spring. Localmente o servidor é o
 * Mailpit do Docker Compose (propriedades {@code spring.mail.*}).
 * <p>
 * Toda {@link MailException} vira {@link FalhaNoEnvioException}, para que o núcleo não conheça o Spring.
 */
@Component
class NotificacaoSmtpAdapter implements NotificacaoGateway {

	private final JavaMailSender envioDeEmail;
	private final String remetente;

	NotificacaoSmtpAdapter(JavaMailSender envioDeEmail,
			@Value("${agendabeleza.notificacoes.remetente}") String remetente) {
		this.envioDeEmail = envioDeEmail;
		this.remetente = remetente;
	}

	@Override
	public void enviar(Mensagem mensagem) {
		SimpleMailMessage email = new SimpleMailMessage();
		email.setFrom(remetente);
		email.setTo(mensagem.destinatario());
		email.setSubject(mensagem.assunto());
		email.setText(mensagem.corpo());
		try {
			envioDeEmail.send(email);
		} catch (MailException falha) {
			throw new FalhaNoEnvioException(mensagem.destinatario(), falha);
		}
	}

}
