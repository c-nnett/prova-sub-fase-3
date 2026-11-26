package br.com.fiap.agendabeleza.api.notificacoes.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import br.com.fiap.agendabeleza.api.agendamentos.application.port.AvisosDoAgendamento;
import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoGateway;
import br.com.fiap.agendabeleza.api.notificacoes.application.port.NotificacaoRepository;
import br.com.fiap.agendabeleza.api.notificacoes.application.usecase.AvisosPorEmail;
import br.com.fiap.agendabeleza.api.notificacoes.application.usecase.EnviarNotificacoesPendentesUseCase;

/**
 * Registra os casos de uso do módulo como beans e liga o agendador do Spring, usado pelo job de envio. A porta
 * {@link AvisosDoAgendamento} pertence ao módulo de agendamentos e é implementada aqui (arquitetura, seção 4.2).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class NotificacoesConfig {

	@Bean
	AvisosDoAgendamento avisosDoAgendamento(NotificacaoRepository notificacoes, Clock relogio) {
		return new AvisosPorEmail(notificacoes, relogio);
	}

	@Bean
	EnviarNotificacoesPendentesUseCase enviarNotificacoesPendentesUseCase(NotificacaoRepository notificacoes,
			NotificacaoGateway servidorDeEmail, Clock relogio) {
		return new EnviarNotificacoesPendentesUseCase(notificacoes, servidorDeEmail, relogio);
	}

}
