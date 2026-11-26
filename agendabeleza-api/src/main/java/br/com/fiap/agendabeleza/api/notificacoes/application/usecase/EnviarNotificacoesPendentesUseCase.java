package br.com.fiap.agendabeleza.api.notificacoes.application.usecase;

import java.time.Clock;

import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoGateway;
import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoRepository;
import br.com.fiap.agendabeleza.api.notificacoes.domain.FalhaNoEnvioException;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;

/**
 * Uma rodada do job de envio (arquitetura, seção 6.2): entrega as pendentes vencidas, até
 * {@value #LIMITE_POR_RODADA} por rodada, e grava o resultado de cada uma. Uma falha conta tentativa e não
 * interrompe as demais.
 * <p>
 * A entrega é "pelo menos uma vez": se a aplicação cair entre o envio e a gravação, o e-mail sai de novo na
 * próxima rodada.
 */
public class EnviarNotificacoesPendentesUseCase {

	public static final int LIMITE_POR_RODADA = 50;

	private final NotificacaoRepository notificacoes;
	private final NotificacaoGateway servidorDeEmail;
	private final Clock relogio;

	public EnviarNotificacoesPendentesUseCase(NotificacaoRepository notificacoes, NotificacaoGateway servidorDeEmail,
			Clock relogio) {
		this.notificacoes = notificacoes;
		this.servidorDeEmail = servidorDeEmail;
		this.relogio = relogio;
	}

	/** @return quantas notificações foram enviadas nesta rodada */
	public int executar() {
		int enviadas = 0;
		for (Notificacao notificacao : notificacoes.pendentesAte(relogio.instant(), LIMITE_POR_RODADA)) {
			if (enviar(notificacao)) {
				enviadas++;
			}
			notificacoes.atualizar(notificacao);
		}
		return enviadas;
	}

	private boolean enviar(Notificacao notificacao) {
		try {
			servidorDeEmail.enviar(notificacao.mensagem());
			notificacao.marcarEnviada(relogio.instant());
			return true;
		} catch (FalhaNoEnvioException falha) {
			notificacao.registrarFalha(motivo(falha));
			return false;
		}
	}

	/** A causa diz o que o servidor respondeu; a mensagem da exceção só repete o destinatário. */
	private static String motivo(FalhaNoEnvioException falha) {
		return falha.getCause() != null ? falha.getCause().getMessage() : falha.getMessage();
	}

}
