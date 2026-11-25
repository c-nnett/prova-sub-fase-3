package br.com.fiap.agendabeleza.api.notificacoes.application.port;

import br.com.fiap.agendabeleza.api.notificacoes.domain.FalhaNoEnvioException;
import br.com.fiap.agendabeleza.api.notificacoes.domain.Mensagem;

/**
 * Porta de saída para entregar e-mails de confirmação e lembrete (funcionalidade 3).
 * <p>
 * Deve ser chamada fora da transação do agendamento, depois do commit: o servidor de e-mail é externo e não pode
 * desfazer nem segurar uma reserva (arquitetura, seção 6.2). A entrega é "pelo menos uma vez".
 */
public interface NotificacaoGateway {

	/**
	 * @param mensagem e-mail já validado
	 * @throws FalhaNoEnvioException se o servidor de e-mail recusar ou estiver fora do ar
	 */
	void enviar(Mensagem mensagem);

}
