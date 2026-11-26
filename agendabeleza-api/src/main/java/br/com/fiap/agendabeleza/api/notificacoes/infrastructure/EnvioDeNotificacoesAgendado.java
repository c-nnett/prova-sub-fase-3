package br.com.fiap.agendabeleza.api.notificacoes.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.fiap.agendabeleza.api.notificacoes.application.usecase.EnviarNotificacoesPendentesUseCase;

/**
 * Job da outbox: a cada intervalo, envia as notificações pendentes vencidas. O intervalo conta do fim de uma rodada
 * ao início da seguinte, então duas rodadas nunca correm juntas nesta instância.
 * <p>
 * Desligável com {@code agendabeleza.notificacoes.envio-automatico=false}, como nos testes, que chamam o caso de
 * uso diretamente.
 */
@Component
@ConditionalOnProperty(name = "agendabeleza.notificacoes.envio-automatico", havingValue = "true",
		matchIfMissing = true)
class EnvioDeNotificacoesAgendado {

	private static final Logger LOG = LoggerFactory.getLogger(EnvioDeNotificacoesAgendado.class);

	private final EnviarNotificacoesPendentesUseCase enviar;

	EnvioDeNotificacoesAgendado(EnviarNotificacoesPendentesUseCase enviar) {
		this.enviar = enviar;
	}

	@Scheduled(fixedDelayString = "${agendabeleza.notificacoes.intervalo-de-envio:PT1M}")
	void enviarPendentes() {
		int enviadas = enviar.executar();
		if (enviadas > 0) {
			LOG.info("{} notificações enviadas", enviadas);
		}
	}

}
