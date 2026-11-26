package br.com.fiap.agendabeleza.api.notificacoes.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.agendabeleza.api.notificacoes.domain.Notificacao;

/** Outbox de e-mails (modelagem, seção 3.6). */
public interface NotificacaoRepository {

	/** Grava notificações novas. Chamado dentro da transação da alteração do agendamento. */
	void salvarTodas(List<Notificacao> notificacoes);

	/** Grava o resultado de uma tentativa de envio. */
	void atualizar(Notificacao notificacao);

	/** Pendentes com {@code enviarEm} até {@code momento}, da mais antiga para a mais recente. */
	List<Notificacao> pendentesAte(Instant momento, int limite);

	/** E-mail do cliente informado na reserva, lido da primeira notificação dele para o agendamento. */
	Optional<String> emailDoCliente(UUID agendamentoId);

}
