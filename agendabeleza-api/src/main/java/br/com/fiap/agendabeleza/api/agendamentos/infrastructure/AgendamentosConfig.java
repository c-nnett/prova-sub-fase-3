package br.com.fiap.agendabeleza.api.agendamentos.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.fiap.agendabeleza.api.agendamentos.application.usecase.ConsultarHorariosLivresUseCase;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.EstabelecimentoRepository;
import br.com.fiap.agendabeleza.api.estabelecimentos.application.port.ServicoRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.BloqueioAgendaRepository;
import br.com.fiap.agendabeleza.api.profissionais.application.port.ProfissionalRepository;

/**
 * Registra os casos de uso do módulo como beans. As portas dos módulos {@code estabelecimentos} e
 * {@code profissionais} são recebidas pela interface (arquitetura, seção 4.2).
 */
@Configuration(proxyBeanMethods = false)
class AgendamentosConfig {

	@Bean
	ConsultarHorariosLivresUseCase consultarHorariosLivresUseCase(ProfissionalRepository profissionais,
			EstabelecimentoRepository estabelecimentos, ServicoRepository servicos,
			BloqueioAgendaRepository bloqueios, Clock relogio) {
		return new ConsultarHorariosLivresUseCase(profissionais, estabelecimentos, servicos, bloqueios, relogio);
	}

}
